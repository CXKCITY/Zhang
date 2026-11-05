package com.spendwise.app.domain
import org.junit.Assert.*
import org.junit.Test
class CategoryOrderRulesTest {
 private val categories = listOf(Category(1,"餐饮",0),Category(2,"交通",0),Category(3,"购物",0))
 @Test fun appliesSavedOrderAndAppendsNewCategories() { assertEquals(listOf(2L,1L,3L),CategoryOrderRules.sort(categories,listOf(2,1)).map { it.id }) }
 @Test fun toleratesDeletedAndRepeatedIds() { assertEquals(listOf(3L,1L,2L),CategoryOrderRules.sort(categories,listOf(9,3,3)).map { it.id }) }
 @Test fun movesWithoutLosingOrDuplicatingCategories() { assertEquals(listOf(2L,1L,3L),CategoryOrderRules.move(listOf(1,2,3),1,-1)); assertEquals(listOf(1L,2L,3L),CategoryOrderRules.move(listOf(1,2,3),0,-1)) }
 @Test fun dragCanMoveAcrossSeveralRows() {
  assertEquals(listOf(2L,3L,4L,1L),CategoryOrderRules.moveTo(listOf(1,2,3,4),0,3))
  assertEquals(listOf(4L,1L,2L,3L),CategoryOrderRules.moveTo(listOf(1,2,3,4),3,0))
 }
 @Test fun everyDragKeepsAllItemsAndMovesToTarget() {
  val ids=listOf(1L,2L,3L,4L,5L)
  ids.indices.forEach { from -> ids.indices.forEach { to ->
   val result=CategoryOrderRules.moveTo(ids,from,to)
   assertEquals(ids.toSet(),result.toSet()); assertEquals(ids.size,result.size); assertEquals(ids[from],result[to])
  } }
  assertEquals(ids,CategoryOrderRules.moveTo(ids,-1,2))
 }
}
