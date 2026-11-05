package com.spendwise.app.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.spendwise.app.domain.Account
import com.spendwise.app.domain.Category
import com.spendwise.app.domain.Expense
import com.spendwise.app.domain.Transfer
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * CSV for one period (a month or a year) — every expense, income entry and
 * transfer in it, oldest first. Transfers carry their own type and a
 * "转入账户" so a spreadsheet total of expenses stays honest.
 *
 * Text cells are quoted, and a cell starting with = + - @ gets a leading
 * apostrophe so spreadsheet apps don't evaluate it as a formula. Amounts stay
 * numeric (expenses negative). UTF-8 with BOM + CRLF, which Excel opens
 * without an import dialog.
 */
object PeriodCsvExporter {

    private const val HEADER = "日期,消费内容,支出金额 (CNY),分类"
    private val DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun buildCsv(
        expenses: List<Expense>,
        transfers: List<Transfer>,
        categories: List<Category>,
        accounts: List<Account>,
        zone: ZoneId
    ): String {
        val categoriesById = categories.associateBy { it.id }
        val accountNames = accounts.associate { it.id to it.name }
        data class Row(val at: Long, val order: Long, val cells: List<String>)

        val rows = expenses.filter { categoriesById[it.categoryId]?.isIncomeAdjustment != true }.map { e ->
            Row(e.occurredAtMillis, e.createdAtMillis, listOf(
                text(date(e.occurredAtMillis, zone)),
                text(e.notes.ifBlank { categoriesById[e.categoryId]?.name ?: e.categoryName }),
                amount(e.amountCents),
                text(categoriesById[e.categoryId]?.name ?: e.categoryName)
            ))
        }
        val body = rows
            .sortedWith(compareBy<Row> { it.at }.thenBy { it.order })
            .joinToString("\r\n") { it.cells.joinToString(",") }
        return "\uFEFF" + HEADER + if (body.isEmpty()) "" else "\r\n" + body
    }

    /** Writes [csv] to the export cache and opens the share sheet. */
    fun share(context: Context, csv: String, fileName: String, subject: String): Boolean =
        runCatching {
            val dir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
            val file = File(dir, fileName)
            file.writeText(csv, Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                // ClipData carries the read grant to the share sheet itself,
                // so it can show the file's name and size, not just to the
                // app the user picks.
                clipData = ClipData.newRawUri(fileName, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(send, subject).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        }.getOrDefault(false)

    private fun date(millis: Long, zone: ZoneId): String =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().format(DATE)

    private fun amount(cents: Long): String {
        val sign = if (cents < 0) "-" else ""
        val abs = if (cents < 0) -cents else cents
        return "$sign${abs / 100}.${(abs % 100).toString().padStart(2, '0')}"
    }

    internal fun text(value: String): String {
        val safe = if (Regex("^\\s*[=+@-]").containsMatchIn(value)) "'$value" else value
        return "\"" + safe.replace("\"", "\"\"") + "\""
    }
}
