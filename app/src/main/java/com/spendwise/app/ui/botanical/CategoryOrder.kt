package com.spendwise.app.ui.botanical

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.spendwise.app.domain.Category
import com.spendwise.app.domain.CategoryOrderRules

object CategoryOrder {
    private var context: Context? = null
    private var ids by mutableStateOf(emptyList<Long>())
    fun load(c: Context) {
        context = c.applicationContext
        ids = c.getSharedPreferences("category_order",0).getString("ids", "").orEmpty().split(',').mapNotNull { it.toLongOrNull()?.takeIf { value -> value > 0 } }.distinct()
    }
    fun sort(categories: List<Category>) = CategoryOrderRules.sort(categories, ids)
    fun save(order: List<Long>) {
        ids = order.filter { it > 0 }.distinct()
        context?.getSharedPreferences("category_order",0)?.edit()?.putString("ids",ids.joinToString(","))?.apply()
    }
}
