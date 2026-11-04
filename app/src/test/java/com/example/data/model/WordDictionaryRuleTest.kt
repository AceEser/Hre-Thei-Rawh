package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordDictionaryRuleTest {

    @Test
    fun getRandomWordChoices_returnsThreeTiers() {
        val choices = WordDictionary.getRandomWordChoices()
        assertEquals(3, choices.size)
        assertEquals("Easy", choices[0].difficulty)
        assertEquals("Medium", choices[1].difficulty)
        assertEquals("Hard", choices[2].difficulty)
        assertTrue(choices.all { it.word.isNotBlank() })
    }

    @Test
    fun validateCustomWord_rules() {
        // Too short
        val (validShort, errShort) = WordDictionary.validateCustomWord("A")
        assertFalse(validShort)
        assertNotNull(errShort)

        // Too long
        val (validLong, errLong) = WordDictionary.validateCustomWord("A".repeat(30))
        assertFalse(validLong)
        assertNotNull(errLong)

        // Invalid characters
        val (validSpecial, errSpecial) = WordDictionary.validateCustomWord("Cat@Dog!")
        assertFalse(validSpecial)
        assertNotNull(errSpecial)

        // Valid single and multi-word
        val (validNormal, errNormal) = WordDictionary.validateCustomWord("Space Rocket")
        assertTrue(validNormal)
        assertEquals(null, errNormal)
    }

    @Test
    fun generateMaskedHint_progressiveReveals() {
        val word = "ELEPHANT"

        // 0% elapsed: all underscores
        val hint0 = WordDictionary.generateMaskedHint(word, 0f)
        assertEquals("_ _ _ _ _ _ _ _", hint0)

        // 45% elapsed: reveals some letters but not all
        val hint45 = WordDictionary.generateMaskedHint(word, 0.45f)
        assertTrue(hint45.contains("_"))
        assertTrue(hint45.any { it.isLetter() })

        // 75% elapsed: reveals more letters, still has underscores
        val hint75 = WordDictionary.generateMaskedHint(word, 0.75f)
        assertTrue(hint75.contains("_"))
        assertTrue(hint75.count { it.isLetter() } >= hint45.count { it.isLetter() })
    }
}
