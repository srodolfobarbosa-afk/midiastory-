package com.rdf112.sweep.domain.analysis

import com.rdf112.sweep.domain.model.MediaKind
import com.rdf112.sweep.domain.model.MediaRecord
import kotlin.math.abs

/**
 * Visual fingerprint for a single on-device photo, combining a 64-bit perceptual difference hash
 * ([dHash64]) with a discrete Laplacian sharpness score ([sharpnessScore]).
 */
data class PhotoVisualFingerprint(
    val record: MediaRecord,
    val dHash64: Long,
    val sharpnessScore: Double,
) {
    init {
        require(record.kind == MediaKind.IMAGE) { "PhotoVisualFingerprint requires MediaKind.IMAGE" }
        require(sharpnessScore >= 0.0 && !sharpnessScore.isNaN()) { "sharpnessScore must be non-negative" }
    }
}

/**
 * A reviewable group of visually similar photos (e.g., camera bursts or repeated shots) where
 * [bestKeep] is automatically protected as the sharpest/highest-detail shot and [candidatesToReview]
 * are presented for explicit user review (never auto-deleted).
 */
data class SimilarPhotoGroup(
    val groupId: String,
    val bestKeep: PhotoVisualFingerprint,
    val candidatesToReview: List<PhotoVisualFingerprint>,
    val maxHammingDistanceInGroup: Int,
) {
    val recoverableBytes: Long
        get() = candidatesToReview.fold(0L) { acc, item -> Math.addExact(acc, item.record.sizeBytes) }

    val totalPhotos: Int
        get() = 1 + candidatesToReview.size

    init {
        require(candidatesToReview.isNotEmpty()) { "SimilarPhotoGroup requires at least 1 review candidate" }
        require(!bestKeep.record.isSelectable && bestKeep.record.isProtected) {
            "bestKeep must be marked protected and non-selectable"
        }
        require(candidatesToReview.none { it.record.uri == bestKeep.record.uri }) {
            "bestKeep cannot also appear in candidatesToReview"
        }
    }
}

/**
 * Pure Kotlin 64-bit perceptual Difference Hash (dHash 9x8 area-averaged luminance) and
 * false-positive-resistant burst/similarity grouper.
 *
 * Zero external dependencies, deterministic across JVM and Android API 29–36.
 */
object PerceptualImageHasher {
    const val GRID_WIDTH = 9
    const val GRID_HEIGHT = 8
    const val MIN_DYNAMIC_LUMA_RANGE = 6
    const val DEFAULT_MAX_HAMMING_DISTANCE = 8
    const val NEAR_IDENTICAL_HAMMING_DISTANCE = 3
    const val DEFAULT_BURST_TIME_WINDOW_SECONDS = 86_400L // 24 hours

    fun hammingDistance(hashA: Long, hashB: Long): Int = (hashA xor hashB).countOneBits()

    fun computeDHash64FromArgb(
        argbPixels: IntArray,
        width: Int,
        height: Int,
    ): Long? {
        if (width < GRID_WIDTH || height < GRID_HEIGHT) return null
        val pixelCount = width.toLong() * height.toLong()
        if (pixelCount <= 0L || argbPixels.size.toLong() < pixelCount) return null

        val luma = IntArray(pixelCount.toInt()) { idx -> BlurVarianceEstimator.rgbPixelToLuma(argbPixels[idx]) }
        return computeDHash64FromLuma(luma, width, height)
    }

    /**
     * Downsamples [luma] (`[0..255]`) into a 9x8 box-averaged grid to suppress sensor/JPEG noise,
     * rejects near-uniform/blank frames (`maxLuma - minLuma < MIN_DYNAMIC_LUMA_RANGE`), and encodes
     * 8x8 horizontal gradient comparisons into a 64-bit [Long].
     */
    fun computeDHash64FromLuma(
        luma: IntArray,
        width: Int,
        height: Int,
    ): Long? {
        if (width < GRID_WIDTH || height < GRID_HEIGHT) return null
        val pixelCount = width.toLong() * height.toLong()
        if (pixelCount <= 0L || luma.size.toLong() < pixelCount) return null

        val grid = DoubleArray(GRID_WIDTH * GRID_HEIGHT)
        var minCell = Double.POSITIVE_INFINITY
        var maxCell = Double.NEGATIVE_INFINITY

        for (gy in 0 until GRID_HEIGHT) {
            val yStart = (gy * height) / GRID_HEIGHT
            val yEnd = (((gy + 1) * height) / GRID_HEIGHT).coerceAtLeast(yStart + 1).coerceAtMost(height)
            for (gx in 0 until GRID_WIDTH) {
                val xStart = (gx * width) / GRID_WIDTH
                val xEnd = (((gx + 1) * width) / GRID_WIDTH).coerceAtLeast(xStart + 1).coerceAtMost(width)

                var sum = 0L
                var count = 0
                for (y in yStart until yEnd) {
                    val rowBase = y * width
                    for (x in xStart until xEnd) {
                        sum += luma[rowBase + x].coerceIn(0, 255)
                        count++
                    }
                }
                val avg = if (count > 0) sum.toDouble() / count.toDouble() else 0.0
                grid[gy * GRID_WIDTH + gx] = avg
                if (avg < minCell) minCell = avg
                if (avg > maxCell) maxCell = avg
            }
        }

        // Fail closed on solid black/white/corrupted thumbnails to avoid false-positive clusters.
        if ((maxCell - minCell) < MIN_DYNAMIC_LUMA_RANGE.toDouble()) {
            return null
        }

        var hash = 0L
        var bitIndex = 0
        for (gy in 0 until GRID_HEIGHT) {
            val rowOffset = gy * GRID_WIDTH
            for (gx in 0 until (GRID_WIDTH - 1)) {
                if (grid[rowOffset + gx] > grid[rowOffset + gx + 1]) {
                    hash = hash or (1L shl bitIndex)
                }
                bitIndex++
            }
        }
        return hash
    }

    /**
     * Groups visually similar photos while guarding against false positives:
     * - Near-identical re-saves/crops (`distance <= NEAR_IDENTICAL_HAMMING_DISTANCE`) match regardless of date.
     * - Moderate visual similarity (`distance <= maxHammingDistance`) requires capture/modification timestamps
     *   within [burstTimeWindowSeconds] (when both timestamps are available).
     * - Within each cluster, the sharpest photo (highest [PhotoVisualFingerprint.sharpnessScore], tie-broken
     *   by largest file size and deterministic URI order) is locked as `isProtected = true, isSelectable = false`.
     */
    fun detectSimilarGroups(
        items: Collection<PhotoVisualFingerprint>,
        maxHammingDistance: Int = DEFAULT_MAX_HAMMING_DISTANCE,
        burstTimeWindowSeconds: Long = DEFAULT_BURST_TIME_WINDOW_SECONDS,
    ): List<SimilarPhotoGroup> {
        require(maxHammingDistance in 0..16) { "maxHammingDistance must be within 0..16" }
        require(burstTimeWindowSeconds >= 0L) { "burstTimeWindowSeconds must be non-negative" }

        val unique = items
            .distinctBy { it.record.uri }
            .sortedWith(
                compareByDescending<PhotoVisualFingerprint> { it.record.dateModifiedEpochSeconds ?: Long.MIN_VALUE }
                    .thenBy { it.record.uri },
            )
        if (unique.size < 2) return emptyList()

        val n = unique.size
        val parent = IntArray(n) { it }
        fun find(x: Int): Int {
            var root = x
            while (parent[root] != root) root = parent[root]
            var cur = x
            while (cur != root) {
                val next = parent[cur]
                parent[cur] = root
                cur = next
            }
            return root
        }
        fun union(a: Int, b: Int) {
            val ra = find(a)
            val rb = find(b)
            if (ra != rb) parent[rb] = ra
        }

        // Bounded sliding window + exact check for near-identical re-saves
        for (i in 0 until n) {
            val a = unique[i]
            val timeA = a.record.dateModifiedEpochSeconds
            for (j in (i + 1) until n) {
                val b = unique[j]
                val dist = hammingDistance(a.dHash64, b.dHash64)
                if (dist > maxHammingDistance) continue

                val isNearIdentical = dist <= NEAR_IDENTICAL_HAMMING_DISTANCE
                val timeB = b.record.dateModifiedEpochSeconds
                val withinWindow = if (timeA != null && timeB != null) {
                    abs(timeA - timeB) <= burstTimeWindowSeconds
                } else {
                    dist <= (NEAR_IDENTICAL_HAMMING_DISTANCE + 2)
                }

                if (isNearIdentical || withinWindow) {
                    union(i, j)
                }
            }
        }

        val clusters = LinkedHashMap<Int, MutableList<PhotoVisualFingerprint>>()
        for (idx in 0 until n) {
            clusters.getOrPut(find(idx)) { ArrayList() }.add(unique[idx])
        }

        val groups = ArrayList<SimilarPhotoGroup>()
        for (members in clusters.values) {
            if (members.size < 2) continue

            // Rank best shot first: highest sharpness, then largest size (most detail), then deterministic URI
            val ranked = members.sortedWith(
                compareByDescending<PhotoVisualFingerprint> { it.sharpnessScore }
                    .thenByDescending { it.record.sizeBytes }
                    .thenBy { it.record.uri },
            )
            val winner = ranked.first()
            val protectedBest = winner.copy(
                record = winner.record.copy(
                    isProtected = true,
                    isSelectable = false,
                ),
            )
            val candidates = ranked.drop(1).map { candidate ->
                candidate.copy(
                    record = candidate.record.copy(
                        isProtected = false,
                        isSelectable = true,
                    ),
                )
            }
            val maxDist = candidates.maxOf { hammingDistance(protectedBest.dHash64, it.dHash64) }

            groups += SimilarPhotoGroup(
                groupId = "sim_${java.lang.Long.toUnsignedString(protectedBest.dHash64, 16)}_${protectedBest.record.uri.hashCode().toUInt().toString(16)}",
                bestKeep = protectedBest,
                candidatesToReview = candidates,
                maxHammingDistanceInGroup = maxDist,
            )
        }

        return groups.sortedWith(
            compareByDescending<SimilarPhotoGroup> { it.recoverableBytes }
                .thenByDescending { it.totalPhotos },
        )
    }
}
