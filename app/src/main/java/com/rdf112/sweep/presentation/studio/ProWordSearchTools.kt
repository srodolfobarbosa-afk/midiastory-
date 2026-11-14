package com.rdf112.sweep.presentation.studio

import java.text.Normalizer
import java.util.Calendar
import java.util.Locale
import java.util.Random

enum class WordSearchDirection(val dRow: Int, val dCol: Int) {
    HORIZONTAL(0, 1),
    VERTICAL(1, 0),
    DIAGONAL_DOWN(1, 1),
    DIAGONAL_UP(-1, 1),
}

data class WordPlacement(
    val word: String,
    val startRow: Int,
    val startCol: Int,
    val endRow: Int,
    val endCol: Int,
)

data class WordSearchPuzzle(
    val size: Int,
    val grid: List<List<Char>>,
    val placements: List<WordPlacement>,
    val themeTitle: String,
    val isDailyChallenge: Boolean,
    val dateKey: String,
)

data class WordSearchPack(
    val titlePt: String,
    val titleEn: String,
    val wordsPt: List<String>,
    val wordsEn: List<String>,
)

/**
 * 100% offline Pro Word Search generator & tap-to-match validator.
 * Zero network dependency: generates deterministic Daily Challenges by local date or random rounds.
 */
object ProWordSearchTools {

    private val PACKS: List<WordSearchPack> = listOf(
        WordSearchPack(
            titlePt = "Mundo & Viagem",
            titleEn = "World & Travel",
            wordsPt = listOf("BRASIL", "JAPAO", "INDIA", "EGITO", "AVIAO", "MAPA", "PRAIA", "HOTEL", "TREM", "SOL"),
            wordsEn = listOf("BRAZIL", "JAPAN", "INDIA", "EGYPT", "PLANE", "MAP", "BEACH", "HOTEL", "TRAIN", "SUN"),
        ),
        WordSearchPack(
            titlePt = "Limpeza & Offline",
            titleEn = "Offline Cleanup",
            wordsPt = listOf("MEMORIA", "OFFLINE", "FOTOS", "VIDEO", "AUDIO", "SENHA", "LIMPO", "BUSCA", "DADOS", "NUVEM"),
            wordsEn = listOf("MEMORY", "OFFLINE", "PHOTO", "VIDEO", "AUDIO", "CLEAN", "SEARCH", "DATA", "VAULT", "CLOUD"),
        ),
        WordSearchPack(
            titlePt = "Mágica & Natureza",
            titleEn = "Magic & Nature",
            wordsPt = listOf("MAGICA", "CARTA", "DADO", "ESTRELA", "VENTO", "TERRA", "OURO", "VERDE", "LUA", "RIO"),
            wordsEn = listOf("MAGIC", "CARDS", "DICE", "STAR", "WIND", "EARTH", "GOLD", "GREEN", "MOON", "RIVER"),
        ),
    )

    fun dailyDateKey(nowMillis: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        return String.format(Locale.US, "%04d-%02d-%02d", y, m, d)
    }

    fun generateDailyPuzzle(
        isPortuguese: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
        size: Int = 9,
        targetWordCount: Int = 6,
    ): WordSearchPuzzle {
        val dateKey = dailyDateKey(nowMillis)
        val seed = dateKey.hashCode().toLong() * 31L + if (isPortuguese) 7L else 19L
        val packIdx = Math.floorMod(dateKey.hashCode(), PACKS.size)
        return generateFromPack(
            pack = PACKS[packIdx],
            isPortuguese = isPortuguese,
            seed = seed,
            isDailyChallenge = true,
            dateKey = dateKey,
            size = size,
            targetWordCount = targetWordCount,
        )
    }

    fun generateRandomPuzzle(
        isPortuguese: Boolean,
        seed: Long = System.nanoTime(),
        size: Int = 9,
        targetWordCount: Int = 6,
    ): WordSearchPuzzle {
        val rng = Random(seed)
        val pack = PACKS[rng.nextInt(PACKS.size)]
        return generateFromPack(
            pack = pack,
            isPortuguese = isPortuguese,
            seed = seed,
            isDailyChallenge = false,
            dateKey = dailyDateKey(),
            size = size,
            targetWordCount = targetWordCount,
        )
    }

    fun generateFromPack(
        pack: WordSearchPack,
        isPortuguese: Boolean,
        seed: Long,
        isDailyChallenge: Boolean,
        dateKey: String,
        size: Int = 9,
        targetWordCount: Int = 6,
    ): WordSearchPuzzle {
        val rng = Random(seed)
        val board = Array(size) { CharArray(size) { '\u0000' } }
        val rawWords = if (isPortuguese) pack.wordsPt else pack.wordsEn
        val candidates = rawWords
            .map { normalizeAsciiWord(it) }
            .filter { it.length in 3..size }
            .shuffled(rng)

        val placements = mutableListOf<WordPlacement>()
        val directions = WordSearchDirection.entries.toList()

        for (word in candidates) {
            if (placements.size >= targetWordCount) break
            val placed = tryPlaceWord(board, size, word, directions, rng)
            if (placed != null) {
                placements += placed
            }
        }

        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (board[r][c] == '\u0000') {
                    board[r][c] = alphabet[rng.nextInt(alphabet.length)]
                }
            }
        }

        val immutableGrid = board.map { row -> row.toList() }
        val title = if (isPortuguese) pack.titlePt else pack.titleEn
        return WordSearchPuzzle(
            size = size,
            grid = immutableGrid,
            placements = placements,
            themeTitle = title,
            isDailyChallenge = isDailyChallenge,
            dateKey = dateKey,
        )
    }

    private fun tryPlaceWord(
        board: Array<CharArray>,
        size: Int,
        word: String,
        directions: List<WordSearchDirection>,
        rng: Random,
    ): WordPlacement? {
        val shuffledDirs = directions.shuffled(rng)
        val positions = buildList {
            for (r in 0 until size) {
                for (c in 0 until size) {
                    add(r to c)
                }
            }
        }.shuffled(rng)

        for (dir in shuffledDirs) {
            for ((sr, sc) in positions) {
                val er = sr + dir.dRow * (word.length - 1)
                val ec = sc + dir.dCol * (word.length - 1)
                if (er !in 0 until size || ec !in 0 until size) continue

                var fits = true
                for (i in word.indices) {
                    val rr = sr + dir.dRow * i
                    val cc = sc + dir.dCol * i
                    val existing = board[rr][cc]
                    if (existing != '\u0000' && existing != word[i]) {
                        fits = false
                        break
                    }
                }
                if (!fits) continue

                for (i in word.indices) {
                    val rr = sr + dir.dRow * i
                    val cc = sc + dir.dCol * i
                    board[rr][cc] = word[i]
                }
                return WordPlacement(
                    word = word,
                    startRow = sr,
                    startCol = sc,
                    endRow = er,
                    endCol = ec,
                )
            }
        }
        return null
    }

    fun findMatchingWord(
        puzzle: WordSearchPuzzle,
        r1: Int,
        c1: Int,
        r2: Int,
        c2: Int,
        alreadyFoundWords: Set<String>,
    ): WordPlacement? {
        return puzzle.placements.firstOrNull { p ->
            p.word !in alreadyFoundWords && (
                (p.startRow == r1 && p.startCol == c1 && p.endRow == r2 && p.endCol == c2) ||
                    (p.endRow == r1 && p.endCol == c1 && p.startRow == r2 && p.startCol == c2)
                )
        }
    }

    fun cellsForPlacement(p: WordPlacement): Set<Pair<Int, Int>> {
        val len = maxOf(kotlin.math.abs(p.endRow - p.startRow), kotlin.math.abs(p.endCol - p.startCol)) + 1
        val dR = Integer.compare(p.endRow, p.startRow)
        val dC = Integer.compare(p.endCol, p.startCol)
        return buildSet {
            for (i in 0 until len) {
                add((p.startRow + dR * i) to (p.startCol + dC * i))
            }
        }
    }

    private fun normalizeAsciiWord(raw: String): String {
        val decomposed = Normalizer.normalize(raw.uppercase(Locale.US), Normalizer.Form.NFD)
        return decomposed.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .filter { it in 'A'..'Z' }
    }
}
