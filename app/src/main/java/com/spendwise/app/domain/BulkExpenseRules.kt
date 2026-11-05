package com.spendwise.app.domain

object BulkExpenseRules {
    fun validate(ids: Set<Long>, records: Map<Long, Long>, incomeIds: Set<Long>, expenseCategoryIds: Set<Long>, target: Long?) {
        require(ids.isNotEmpty() && ids.all { it > 0 }) { "请选择需要管理的记录" }
        require(records.keys == ids) { "部分记录已变化 请重新选择" }
        require(records.values.none { it in incomeIds } && records.values.all { it in expenseCategoryIds }) { "只能管理消费支出记录" }
        require(target == null || target in expenseCategoryIds) { "目标分类已不存在 请重新选择" }
    }
}

object CategoryOrderRules {
    fun sort(categories: List<Category>, ids: List<Long>): List<Category> {
        val ranks = ids.distinct().withIndex().associate { it.value to it.index }
        return categories.withIndex().sortedWith(compareBy<IndexedValue<Category>> { ranks[it.value.id] ?: Int.MAX_VALUE }.thenBy { it.index }).map { it.value }
    }
    fun move(ids: List<Long>, index: Int, delta: Int): List<Long> = moveTo(ids, index, index + delta)
    fun moveTo(ids: List<Long>, from: Int, to: Int): List<Long> {
        if (from !in ids.indices || to !in ids.indices || from == to) return ids
        return ids.toMutableList().apply { val id=removeAt(from); add(to,id) }
    }
}
