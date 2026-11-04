package com.example.data.model

import kotlin.random.Random

data class WordChoice(
    val word: String,
    val difficulty: String,
    val hintCategory: String
)

object WordDictionary {
    val easyWords = listOf(
        "Cat", "Sun", "Tree", "House", "Car", "Fish", "Apple", "Book",
        "Star", "Ball", "Boat", "Bird", "Door", "Moon", "Shoe", "Hat",
        "Cup", "Clock", "Flower", "Smile", "Cake", "Key", "Heart", "Eye"
    )

    val mediumWords = listOf(
        "Guitar", "Bicycle", "Rocket", "Castle", "Bridge", "Camera", "Helicopter",
        "Spider", "Volcano", "Rainbow", "Compass", "Anchor", "Telescope",
        "Dolphin", "Pirate", "Dragon", "Kangaroo", "Pyramid", "Submarine", "Lantern"
    )

    val hardWords = listOf(
        "Constellation", "Avalanche", "Spaceship", "Archaeologist", "Metropolis",
        "Time Machine", "Black Hole", "Microscope", "Chameleon", "Lighthouse",
        "Subway Station", "Solar Eclipse", "Treasure Map", "Hovercraft", "Windmill"
    )

    fun getRandomWordChoices(): List<WordChoice> {
        val easy = easyWords.random()
        val medium = mediumWords.random()
        val hard = hardWords.random()
        return listOf(
            WordChoice(easy, "Easy", "Basic item or animal"),
            WordChoice(medium, "Medium", "Complex object or vehicle"),
            WordChoice(hard, "Hard", "Scenic or challenging concept")
        )
    }

    /**
     * Validates a custom word entered by the user.
     * Returns a pair of (isValid, errorReason).
     */
    fun validateCustomWord(input: String): Pair<Boolean, String?> {
        val trimmed = input.trim()
        if (trimmed.length < 2) return false to "Word is too short (min 2 characters)"
        if (trimmed.length > 24) return false to "Word is too long (max 24 characters)"
        if (!trimmed.all { it.isLetter() || it == ' ' || it == '-' }) {
            return false to "Only letters, spaces, and hyphens allowed"
        }
        return true to null
    }

    /**
     * Generates masked hint representation.
     * For example for "ROCKET" with 0 letters revealed: "_ _ _ _ _ _"
     * With 2 letters revealed: "R _ _ K _ _"
     * Time fraction: 0.0 to 1.0 (e.g. 0.33 = 1/3 of round elapsed)
     */
    fun generateMaskedHint(word: String, elapsedFraction: Float): String {
        if (word.isBlank()) return ""
        val cleanWord = word.trim()
        val totalLetters = cleanWord.count { it.isLetter() }
        
        // At 0% elapsed: 0 hints
        // At 30% elapsed: 1 hint
        // At 60% elapsed: 2 hints (max up to ~40% of letters, never reveal full word)
        val maxHints = ((totalLetters * 0.45f).toInt()).coerceAtLeast(1)
        val hintsToReveal = when {
            elapsedFraction >= 0.70f -> maxHints
            elapsedFraction >= 0.40f -> (maxHints / 2).coerceAtLeast(1)
            else -> 0
        }

        // Use deterministic seed based on word length to reveal consistent indices
        val letterIndices = cleanWord.indices.filter { cleanWord[it].isLetter() }
        val random = Random(cleanWord.lowercase().hashCode())
        val revealedIndices = letterIndices.shuffled(random).take(hintsToReveal).toSet()

        return cleanWord.mapIndexed { index, char ->
            when {
                !char.isLetter() -> char.toString()
                revealedIndices.contains(index) -> char.uppercase()
                else -> "_"
            }
        }.joinToString(" ")
    }
}
