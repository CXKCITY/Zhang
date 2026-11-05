package com.spendwise.app.domain
import org.junit.Assert.*
import org.junit.Test
class NoteSuggestionRulesTest {
 @Test fun trimsQueryWithoutChangingUserPhrases() { assertEquals("地铁",NoteSuggestionRules.query("　 地铁 \n")); assertTrue(NoteSuggestionRules.matches("地铁通勤"," 铁 ")); assertTrue(NoteSuggestionRules.matches("Metro","MET")) }
 @Test fun treatsPercentAndUnderscoreAsOrdinaryText() { assertFalse(NoteSuggestionRules.matches("地铁","%")); assertTrue(NoteSuggestionRules.matches("100%咖啡","%")); assertFalse(NoteSuggestionRules.matches("咖啡","_")) }
 @Test fun boundsQueryLengthAndMatchesEmptyInput() { assertEquals(1000,NoteSuggestionRules.query("x".repeat(1100)).length); assertTrue(NoteSuggestionRules.matches("地铁", "")) }
}
