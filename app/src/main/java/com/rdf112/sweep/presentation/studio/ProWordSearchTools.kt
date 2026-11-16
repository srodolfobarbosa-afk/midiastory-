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
            wordsPt = listOf("BRASIL", "JAPAO", "EGITO", "PERU", "CHILE", "AVIAO", "MAPA", "PRAIA", "HOTEL", "TREM", "MALA", "ILHA", "NAVIO", "ROTA", "GUIA", "PORTO"),
            wordsEn = listOf("BRAZIL", "JAPAN", "EGYPT", "PERU", "CHILE", "PLANE", "MAP", "BEACH", "HOTEL", "TRAIN", "SUITCASE", "ISLAND", "SHIP", "ROUTE", "GUIDE", "PORT"),
        ),
        WordSearchPack(
            titlePt = "Animais & Selva",
            titleEn = "Animals & Wildlife",
            wordsPt = listOf("LEAO", "GIRAFA", "TIGRE", "ARARA", "AGUIA", "BALEIA", "GATO", "LOBO", "URSO", "COELHO", "RAPOSA", "CORVO", "PEIXE", "ZEBRA", "PANDA"),
            wordsEn = listOf("LION", "GIRAFFE", "TIGER", "MACAW", "EAGLE", "WHALE", "CAT", "WOLF", "BEAR", "RABBIT", "FOX", "CROW", "FISH", "ZEBRA", "PANDA"),
        ),
        WordSearchPack(
            titlePt = "Frutas & Sabor",
            titleEn = "Fruits & Flavor",
            wordsPt = listOf("BANANA", "MACA", "LARANJA", "UVA", "MANGA", "MORANGO", "LIMAO", "PERA", "CAFE", "PAO", "QUEIJO", "ARROZ", "FEIJAO", "SUCO", "BOLO", "SOPA"),
            wordsEn = listOf("BANANA", "APPLE", "ORANGE", "GRAPE", "MANGO", "BERRY", "LEMON", "PEAR", "COFFEE", "BREAD", "CHEESE", "RICE", "BEAN", "JUICE", "CAKE", "SOUP"),
        ),
        WordSearchPack(
            titlePt = "Universo & Céu",
            titleEn = "Cosmos & Stars",
            wordsPt = listOf("ESTRELA", "PLANETA", "GALAXIA", "COMETA", "METEORO", "LUA", "TERRA", "MARTE", "JUPITER", "SATURNO", "ORBITA", "FOGUETE", "COSMOS", "ECLIPSE", "NEBULA"),
            wordsEn = listOf("STAR", "PLANET", "GALAXY", "COMET", "METEOR", "MOON", "EARTH", "MARS", "JUPITER", "SATURN", "ORBIT", "ROCKET", "COSMOS", "ECLIPSE", "NEBULA"),
        ),
        WordSearchPack(
            titlePt = "Esportes & Ação",
            titleEn = "Sports & Champions",
            wordsPt = listOf("FUTEBOL", "BASQUETE", "VOLEI", "NATACAO", "CORRIDA", "TENIS", "SURFE", "BOXE", "MEDALHA", "TROFEU", "CAMPEAO", "ESTADIO", "GOL", "PISTA", "ATLETA", "BOLA"),
            wordsEn = listOf("SOCCER", "BASKET", "VOLLEY", "SWIM", "RUNNING", "TENNIS", "SURF", "BOXING", "MEDAL", "TROPHY", "CHAMP", "STADIUM", "GOAL", "TRACK", "ATHLETE", "BALL"),
        ),
        WordSearchPack(
            titlePt = "Cidades & Capitais",
            titleEn = "Cities & Capitals",
            wordsPt = listOf("LISBOA", "ROMA", "PARIS", "MADRI", "LONDRES", "BERLIM", "TOQUIO", "PEQUIM", "MOSCOU", "CAIRO", "BRASILIA", "MANAUS", "RECIFE", "NATAL", "PORTO"),
            wordsEn = listOf("LISBON", "ROME", "PARIS", "MADRID", "LONDON", "BERLIN", "TOKYO", "BEIJING", "MOSCOW", "CAIRO", "BRASILIA", "MANAUS", "RECIFE", "NATAL", "PORTO"),
        ),
        WordSearchPack(
            titlePt = "Música & Harmonia",
            titleEn = "Music & Melody",
            wordsPt = listOf("PIANO", "VIOLAO", "FLAUTA", "TAMBOR", "CANCAO", "RITMO", "MELODIA", "TEATRO", "PINTURA", "POESIA", "MUSEU", "PINCEL", "TELA", "ACORDE", "NOTA", "CORO"),
            wordsEn = listOf("PIANO", "GUITAR", "FLUTE", "DRUM", "SONG", "RHYTHM", "MELODY", "THEATER", "PAINTING", "POETRY", "MUSEUM", "BRUSH", "CANVAS", "CHORD", "NOTE", "CHOIR"),
        ),
        WordSearchPack(
            titlePt = "Tecnologia & Futuro",
            titleEn = "Tech & Future",
            wordsPt = listOf("CODIGO", "ROBOTICA", "CHIP", "SENSOR", "REDE", "NUVEM", "DADOS", "LOGICA", "ELETRON", "ATOMO", "FISICA", "QUIMICA", "ENERGIA", "SISTEMA", "LASER", "LENTES"),
            wordsEn = listOf("CODE", "ROBOT", "CHIP", "SENSOR", "NETWORK", "CLOUD", "DATA", "LOGIC", "ELECTRON", "ATOM", "PHYSICS", "CHEMISTRY", "ENERGY", "SYSTEM", "LASER", "LENS"),
        ),
        WordSearchPack(
            titlePt = "Profissões de Valor",
            titleEn = "Great Professions",
            wordsPt = listOf("MEDICO", "MESTRE", "PILOTO", "JUIZ", "BOMBEIRO", "PINTOR", "ESCRITOR", "BIOLOGO", "MARUJO", "ARTISTA", "MUSICO", "GUIA", "CIENCIA", "HEROI"),
            wordsEn = listOf("DOCTOR", "TEACHER", "PILOT", "JUDGE", "PAINTER", "WRITER", "BIOLOGIST", "SAILOR", "ARTIST", "MUSICIAN", "GUIDE", "HERO", "NURSE", "SCIENTIST"),
        ),
        WordSearchPack(
            titlePt = "Virtudes & Paz",
            titleEn = "Virtues & Peace",
            wordsPt = listOf("ALEGRIA", "CORAGEM", "BONDADE", "ESPERANCA", "HARMONIA", "PAZ", "AMOR", "GRATIDAO", "AMIZADE", "RESPEITO", "EMPATIA", "VONTADE", "CARINHO", "FORCA", "UNIAO", "LUZ"),
            wordsEn = listOf("JOY", "COURAGE", "KINDNESS", "HOPE", "HARMONY", "PEACE", "LOVE", "THANKS", "FRIEND", "RESPECT", "EMPATHY", "WILL", "AFFECTION", "STRENGTH", "UNITY", "LIGHT"),
        ),
        WordSearchPack(
            titlePt = "Flores & Jardins",
            titleEn = "Flowers & Garden",
            wordsPt = listOf("ROSA", "ORQUIDEA", "GIRASSOL", "LIRIO", "VIOLETA", "JASMIM", "TULIPA", "CRAVO", "CAMELIA", "BEGONIA", "PETALAS", "JARDIM", "POLEN", "AROMA", "FOLHA", "RAMO"),
            wordsEn = listOf("ROSE", "ORCHID", "SUNFLOWER", "LILY", "VIOLET", "JASMINE", "TULIP", "CLOVE", "CAMELIA", "BEGONIA", "PETAL", "GARDEN", "POLLEN", "AROMA", "LEAF", "BRANCH"),
        ),
        WordSearchPack(
            titlePt = "História & Civilizações",
            titleEn = "History & Empires",
            wordsPt = listOf("IMPERIO", "REINO", "CASTELO", "COROA", "ESPADA", "TEMPLO", "PIRAMIDE", "FAROL", "EPOCA", "MITOLOGIA", "ROMANOS", "GREGOS", "INCAS", "FARAO", "LEGADO", "MEMORIA"),
            wordsEn = listOf("EMPIRE", "KINGDOM", "CASTLE", "CROWN", "SWORD", "TEMPLE", "PYRAMID", "LIGHTHOUSE", "EPOCH", "MYTH", "ROMANS", "GREEKS", "INCAS", "PHARAOH", "LEGACY", "MEMORY"),
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
