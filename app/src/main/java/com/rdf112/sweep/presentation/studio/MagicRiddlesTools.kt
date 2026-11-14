package com.rdf112.sweep.presentation.studio

import android.content.Context
import java.text.Normalizer
import kotlin.random.Random

data class MagicRiddle(
    val id: Int,
    val questionEn: String,
    val questionPt: String,
    val hintEn: String,
    val hintPt: String,
    val acceptedAnswers: Set<String>,
    val rewardEn: String,
    val rewardPt: String,
)

/**
 * Pure offline Curiosity, Magic Tricks & Riddle Unlock engine.
 * Works 100% without internet (classic mathematical 21-card mind reader + magic dice).
 */
object MagicRiddlesTools {

    private const val PREFS_NAME = "midiastore_magic_secrets"
    private const val KEY_UNLOCK_PREFIX = "riddle_unlocked_v1_"

    val RIDDLES: List<MagicRiddle> = listOf(
        MagicRiddle(
            id = 0,
            questionEn = "Riddle 1: I have cities without houses, mountains without trees, and oceans without fish. What am I?",
            questionPt = "Charada 1: Tenho cidades sem casas, montanhas sem árvores e água sem peixes. O que sou eu?",
            hintEn = "Travelers use it to find routes around the world (3 or 4 letters).",
            hintPt = "Viajantes usam para encontrar caminhos e países (4 letras).",
            acceptedAnswers = setOf("map", "a map", "mapa", "o mapa", "atlas"),
            rewardEn = "Unlocks 🃏 21-Card Mind Reader Trick",
            rewardPt = "Desbloqueia 🃏 Mágica das 21 Cartas",
        ),
        MagicRiddle(
            id = 1,
            questionEn = "Riddle 2: The more you clean or take away from me, the bigger I get. What am I?",
            questionPt = "Charada 2: Quanto mais você tira de mim, maior eu fico. O que sou eu?",
            hintEn = "In daily life it is a hole; on your phone it is free space!",
            hintPt = "No dia a dia é um buraco; no seu celular é o espaço livre!",
            acceptedAnswers = setOf("hole", "a hole", "buraco", "um buraco", "space", "free space", "espaco", "espaco livre"),
            rewardEn = "Unlocks 🎲 Magic Dice & Illusionist Mode",
            rewardPt = "Desbloqueia 🎲 Dados Mágicos & Modo Ilusionista",
        ),
        MagicRiddle(
            id = 2,
            questionEn = "Riddle 3: I have keys that open no doors, carry thousands of photos without a brain, and cross oceans inside your pocket. What am I?",
            questionPt = "Charada 3: Tenho teclas sem música, guardo milhares de lembranças sem cérebro e atravesso oceanos no seu bolso. O que sou?",
            hintEn = "You are holding it right now!",
            hintPt = "Você está segurando ele na mão agora mesmo!",
            acceptedAnswers = setOf("phone", "a phone", "smartphone", "cellphone", "mobile", "celular", "o celular", "telefone", "midiastore"),
            rewardEn = "Unlocks 🏆 Master Explorer Secret Badge",
            rewardPt = "Desbloqueia 🏆 Selo Secreto Mestre Explorador",
        ),
    )

    fun normalizeAnswer(input: String): String {
        val decomposed = Normalizer.normalize(input.trim().lowercase(), Normalizer.Form.NFD)
        return decomposed
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace(Regex("[^a-z0-9 ]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun matchesRiddle(riddleId: Int, rawInput: String): Boolean {
        val riddle = RIDDLES.firstOrNull { it.id == riddleId } ?: return false
        val normalized = normalizeAnswer(rawInput)
        return normalized.isNotEmpty() && riddle.acceptedAnswers.contains(normalized)
    }

    fun isRiddleUnlocked(context: Context, riddleId: Int): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean("$KEY_UNLOCK_PREFIX$riddleId", false)

    fun tryUnlockRiddle(context: Context, riddleId: Int, rawInput: String): Boolean {
        if (!matchesRiddle(riddleId, rawInput)) return false
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("$KEY_UNLOCK_PREFIX$riddleId", true)
            .apply()
        return true
    }

    fun unlockedCount(context: Context): Int = RIDDLES.count { isRiddleUnlocked(context, it.id) }
}

/**
 * Classic mathematical 21-card mind-reading illusion (100% deterministic invariant in 3 rounds).
 */
class TwentyOneCardTrickEngine {

    companion object {
        val DEFAULT_DECK: List<String> = listOf(
            "♠️ A", "♥️ K", "♦️ Q", "♣️ J", "♠️ 10", "♥️ 9", "♦️ 8",
            "♣️ 7", "♠️ 6", "♥️ 5", "♦️ 4", "♣️ 3", "♠️ 2", "♥️ A",
            "♦️ K", "♣️ Q", "♠️ J", "♥️ 10", "♦️ 9", "♣️ 8", "♠️ 7",
        )
    }

    private var deck: MutableList<String> = DEFAULT_DECK.toMutableList()
    var round: Int = 1
        private set

    fun reset(shuffleOrder: Boolean = false) {
        deck = if (shuffleOrder) DEFAULT_DECK.shuffled().toMutableList() else DEFAULT_DECK.toMutableList()
        round = 1
    }

    /** Deals all 21 cards left-to-right into 3 columns of 7 cards each. */
    fun dealColumns(): List<List<String>> {
        val col0 = mutableListOf<String>()
        val col1 = mutableListOf<String>()
        val col2 = mutableListOf<String>()
        deck.forEachIndexed { idx, card ->
            when (idx % 3) {
                0 -> col0.add(card)
                1 -> col1.add(card)
                else -> col2.add(card)
            }
        }
        return listOf(col0, col1, col2)
    }

    /**
     * Records which column (0..2) holds the user's secret card, places it strictly in the middle,
     * and returns the revealed card once round 3 finishes (or null while rounds continue).
     */
    fun chooseColumn(colIndex: Int): String? {
        require(colIndex in 0..2) { "Column index must be 0..2" }
        val cols = dealColumns()
        val firstOther = cols[(colIndex + 1) % 3]
        val secondOther = cols[(colIndex + 2) % 3]
        val newDeck = ArrayList<String>(21)
        newDeck.addAll(firstOther)
        newDeck.addAll(cols[colIndex])
        newDeck.addAll(secondOther)
        deck = newDeck
        return if (round >= 3) {
            round = 4
            deck[10]
        } else {
            round += 1
            null
        }
    }
}

object MagicDiceEngine {
    private val GLYPHS = listOf("⚀", "⚁", "⚂", "⚃", "⚄", "⚅")

    fun faceGlyph(value: Int): String = GLYPHS.getOrElse(value - 1) { "🎲" }

    fun rollTwoDice(secretTargetSum: Int? = null): Pair<Int, Int> {
        if (secretTargetSum != null && secretTargetSum in 2..12) {
            val validPairs = mutableListOf<Pair<Int, Int>>()
            for (d1 in 1..6) {
                val d2 = secretTargetSum - d1
                if (d2 in 1..6) validPairs.add(d1 to d2)
            }
            if (validPairs.isNotEmpty()) {
                return validPairs[Random.nextInt(validPairs.size)]
            }
        }
        return Random.nextInt(1, 7) to Random.nextInt(1, 7)
    }
}
