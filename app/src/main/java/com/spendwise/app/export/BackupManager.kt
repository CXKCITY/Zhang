package com.spendwise.app.export

import android.content.Context
import android.net.Uri
import android.util.AtomicFile
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.room.withTransaction
import com.spendwise.app.data.AccountDao
import com.spendwise.app.data.AccountEntity
import com.spendwise.app.data.AppearancePreferenceStore
import com.spendwise.app.data.BudgetDao
import com.spendwise.app.data.BudgetEntity
import com.spendwise.app.data.CategoryDao
import com.spendwise.app.data.CategoryEntity
import com.spendwise.app.data.ExpenseDao
import com.spendwise.app.data.ExpenseDatabase
import com.spendwise.app.data.ExpenseEntity
import com.spendwise.app.data.RecurringRuleDao
import com.spendwise.app.data.RecurringRuleEntity
import com.spendwise.app.data.TransferDao
import com.spendwise.app.data.TransferEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

// Result of an export or import attempt, surfaced to the UI so the settings
// sheet can show a snackbar with a clear message. Success carries a count so
// the toast can be specific ("Backed up 42 entries") rather than a vague
// "Done".
sealed interface BackupResult {
    data class ExportSuccess(val expenseCount: Int) : BackupResult
    data class ImportSuccess(val expenseCount: Int) : BackupResult
    data class Failure(val reason: String) : BackupResult
}

/** What a backup file holds, read before the user agrees to merge it. */
sealed interface BackupPreview {
    data class Ready(
        val entries: Int,
        val transfers: Int,
        val accounts: Int,
        val categories: Int,
        val recurringRules: Int,
        val exportedAtMillis: Long
    ) : BackupPreview

    data class Invalid(val reason: String) : BackupPreview
}

// Room schema version recorded in the backup so future migrations can branch
// on it if needed. Kept as a const here (rather than reading from the
// database) because it's part of the static contract for backups produced by
// this build.
private const val APP_DB_VERSION_AT_EXPORT: Int = 8

class BackupManager(
    private val context: Context,
    private val database: ExpenseDatabase,
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao,
    private val budgetDao: BudgetDao,
    private val recurringRuleDao: RecurringRuleDao,
    private val transferDao: TransferDao,
    private val appearancePreferenceStore: AppearancePreferenceStore
) {
    // prettyPrint makes the file inspectable by a curious user opening it in
    // a text editor; ignoreUnknownKeys is forward-compat so an older app can
    // open a backup written by a newer one (as long as schemaVersion still
    // matches).
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val startupBackupLock = Mutex()
    private val startupBackup = AtomicFile(File(context.filesDir, "qingji-latest-backup.json"))

    fun startupBackupUri(): Uri = Uri.fromFile(startupBackup.baseFile)

    suspend fun saveStartupBackup() = withContext(Dispatchers.IO) {
        startupBackupLock.withLock {
            val envelope = snapshot()
            val stream = startupBackup.startWrite()
            try {
                stream.write(json.encodeToString(BackupEnvelope.serializer(), envelope).toByteArray(Charsets.UTF_8))
                startupBackup.finishWrite(stream)
            } catch (failure: Exception) {
                startupBackup.failWrite(stream)
                throw failure
            }
        }
    }

    private suspend fun snapshot(): BackupEnvelope {
        val darkMode = appearancePreferenceStore.startupDarkModePreference.first()
        return database.withTransaction {
            BackupEnvelope(
                appDbVersion = APP_DB_VERSION_AT_EXPORT,
                exportedAtMillis = System.currentTimeMillis(),
                darkModeEnabled = darkMode,
                categories = categoryDao.allOnce().map { it.toBackup() },
                accounts = accountDao.allOnce().map { it.toBackup() },
                budgets = budgetDao.allOnce().map { it.toBackup() },
                expenses = expenseDao.allOnce().map { it.toBackup() },
                recurringRules = recurringRuleDao.allOnce().map { it.toBackup() },
                transfers = transferDao.allOnce().map { it.toBackup() }
            )
        }
    }

    suspend fun exportBackup(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            val envelope = snapshot()

            val payload = json.encodeToString(BackupEnvelope.serializer(), envelope)
            val stream = context.contentResolver.openOutputStream(uri, "wt")
                ?: return@withContext BackupResult.Failure("无法打开所选文件")
            stream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

            BackupResult.ExportSuccess(expenseCount = envelope.expenses.size)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            BackupResult.Failure("备份失败 ${e.message ?: e.javaClass.simpleName}")
        }
    }

    /**
     * Reads [uri] without touching the database so the restore review can
     * show what the file holds before anything is added.
     */
    suspend fun previewBackup(uri: Uri): BackupPreview = withContext(Dispatchers.IO) {
        try {
            when (val read = readEnvelope(uri)) {
                is EnvelopeRead.Failed -> BackupPreview.Invalid(read.reason)
                is EnvelopeRead.Ok -> with(read.envelope) {
                    BackupPreview.Ready(
                        entries = expenses.size,
                        transfers = transfers.size,
                        accounts = accounts.size,
                        categories = categories.size,
                        recurringRules = recurringRules.size,
                        exportedAtMillis = exportedAtMillis
                    )
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            BackupPreview.Invalid("无法读取备份 ${e.message ?: e.javaClass.simpleName}")
        }
    }

    private sealed interface EnvelopeRead {
        data class Ok(val envelope: BackupEnvelope) : EnvelopeRead
        data class Failed(val reason: String) : EnvelopeRead
    }

    private suspend fun readEnvelope(uri: Uri): EnvelopeRead {
        val bytes = if (uri == startupBackupUri()) {
            startupBackupLock.withLock { startupBackup.readFully() }
        } else {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }
        val text = bytes?.toString(Charsets.UTF_8)?.removePrefix("\uFEFF") ?: return EnvelopeRead.Failed("无法打开所选文件")

        val envelope = try {
            json.decodeFromString(BackupEnvelope.serializer(), text)
        } catch (_: Exception) {
            return EnvelopeRead.Failed("所选文件不是有效的账本备份")
        }

        if (envelope.schemaVersion > CURRENT_BACKUP_SCHEMA_VERSION) {
            return EnvelopeRead.Failed(
                "此备份来自更新版本 请先更新应用"
            )
        }
        return EnvelopeRead.Ok(envelope)
    }

    suspend fun importBackup(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            val envelope = when (val read = readEnvelope(uri)) {
                is EnvelopeRead.Failed -> return@withContext BackupResult.Failure(read.reason)
                is EnvelopeRead.Ok -> read.envelope
            }

            val added = database.withTransaction { merge(envelope) }
            BackupResult.ImportSuccess(expenseCount = added)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            BackupResult.Failure("恢复失败 ${e.message ?: e.javaClass.simpleName}")
        }
    }
    private suspend fun merge(envelope: BackupEnvelope): Int {
        val categories = categoryDao.allOnce().toMutableList()
        var nextCategoryId = (categories.maxOfOrNull { it.id } ?: 0L) + 1
        val categoryIds = envelope.categories.associate { incoming ->
            val existing = categories.firstOrNull {
                it.isIncomeAdjustment == incoming.isIncomeAdjustment &&
                    it.name.trim().equals(incoming.name.trim(), ignoreCase = true)
            }
            val matched = existing ?: incoming.toEntity().copy(id = nextCategoryId++).also {
                categoryDao.insert(it)
                categories.add(it)
            }
            incoming.id to matched.id
        }
        val accounts = accountDao.allOnce().toMutableList()
        val accountIds = envelope.accounts.associate { incoming ->
            val existing = accounts.firstOrNull {
                it.type == incoming.type && it.name.trim().equals(incoming.name.trim(), ignoreCase = true)
            }
            val id = existing?.id ?: accountDao.insert(incoming.toEntity().copy(id = 0)).also {
                accounts.add(incoming.toEntity().copy(id = it))
            }
            incoming.id to id
        }
        val existingBudgets = budgetDao.allOnce().map { it.categoryId }.toMutableSet()
        val budgets = envelope.budgets.map { incoming ->
            incoming.toEntity().copy(id = 0, categoryId = categoryIds.getValue(incoming.categoryId))
        }.filter { existingBudgets.add(it.categoryId) }
        budgetDao.insertAll(budgets)

        val currentExpenses = expenseDao.allOnce()
        val currentById = currentExpenses.associateBy { it.id }
        val (knownExpenses, incomingExpenses) = envelope.expenses.partition { incoming ->
            // Keep local edits when an older copy has the same ID and creation timestamp.
            currentById[incoming.id]?.createdAtMillis == incoming.createdAtMillis
        }
        val knownIds = knownExpenses.mapTo(mutableSetOf()) { it.id }
        val expenses = missingRows(currentExpenses.filterNot { it.id in knownIds }, incomingExpenses.map {
            it.toEntity().copy(id = 0, categoryId = categoryIds.getValue(it.categoryId),
                accountId = accountIds.getValue(it.accountId))
        }) { it.copy(id = 0, createdAtMillis = 0) }
        expenseDao.insertAll(expenses)

        val rules = missingRows(recurringRuleDao.allOnce(), envelope.recurringRules.map {
            it.toEntity().copy(id = 0, categoryId = categoryIds.getValue(it.categoryId),
                accountId = accountIds.getValue(it.accountId))
        }) { it.copy(id = 0, createdAtMillis = 0, nextDueEpochDay = 0, isPaused = false) }
        recurringRuleDao.insertAll(rules)

        val transfers = missingRows(transferDao.allOnce(), envelope.transfers.map {
            it.toEntity().copy(id = 0, fromAccountId = accountIds.getValue(it.fromAccountId),
                toAccountId = accountIds.getValue(it.toAccountId))
        }) { it.copy(id = 0, createdAtMillis = 0) }
        transferDao.insertAll(transfers)
        return expenses.size
    }

}

// Match multiplicities so intentional identical transactions in a backup survive.
internal fun <T, K> missingRows(existing: List<T>, incoming: List<T>, key: (T) -> K): List<T> {
    val remaining = existing.groupingBy(key).eachCount().toMutableMap()
    return incoming.filter { row ->
        val identity = key(row)
        val count = remaining[identity] ?: 0
        if (count > 0) remaining[identity] = count - 1
        count == 0
    }
}

private fun CategoryEntity.toBackup() = BackupCategory(
    id = id,
    name = name,
    color = color,
    iconName = iconName,
    isIncomeAdjustment = isIncomeAdjustment,
    isCustom = isCustom
)

private fun BackupCategory.toEntity() = CategoryEntity(
    id = id,
    name = name,
    color = color,
    iconName = iconName,
    isIncomeAdjustment = isIncomeAdjustment,
    isCustom = isCustom
)

private fun AccountEntity.toBackup() = BackupAccount(
    id = id,
    name = name,
    type = type,
    startingBalanceCents = startingBalanceCents,
    color = color,
    iconName = iconName,
    sortOrder = sortOrder,
    isArchived = isArchived
)

private fun BackupAccount.toEntity() = AccountEntity(
    id = id,
    name = name,
    type = type,
    startingBalanceCents = startingBalanceCents,
    color = color,
    iconName = iconName,
    sortOrder = sortOrder,
    isArchived = isArchived
)

private fun BudgetEntity.toBackup() = BackupBudget(
    id = id,
    categoryId = categoryId,
    monthlyLimitCents = monthlyLimitCents
)

private fun BackupBudget.toEntity() = BudgetEntity(
    id = id,
    categoryId = categoryId,
    monthlyLimitCents = monthlyLimitCents
)

private fun ExpenseEntity.toBackup() = BackupExpense(
    id = id,
    amountCents = amountCents,
    categoryId = categoryId,
    accountId = accountId,
    merchant = merchant,
    notes = notes,
    occurredAtMillis = occurredAtMillis,
    createdAtMillis = createdAtMillis
)

private fun BackupExpense.toEntity() = ExpenseEntity(
    id = id,
    amountCents = amountCents,
    categoryId = categoryId,
    accountId = accountId,
    merchant = merchant,
    notes = notes,
    occurredAtMillis = occurredAtMillis,
    createdAtMillis = createdAtMillis
)

private fun TransferEntity.toBackup() = BackupTransfer(
    id = id,
    fromAccountId = fromAccountId,
    toAccountId = toAccountId,
    amountCents = amountCents,
    notes = notes,
    occurredAtMillis = occurredAtMillis,
    createdAtMillis = createdAtMillis
)

private fun BackupTransfer.toEntity() = TransferEntity(
    id = id,
    fromAccountId = fromAccountId,
    toAccountId = toAccountId,
    amountCents = amountCents,
    notes = notes,
    occurredAtMillis = occurredAtMillis,
    createdAtMillis = createdAtMillis
)

private fun RecurringRuleEntity.toBackup() = BackupRecurringRule(
    id = id,
    amountCents = amountCents,
    categoryId = categoryId,
    accountId = accountId,
    merchant = merchant,
    notes = notes,
    cadence = cadence,
    anchorEpochDay = anchorEpochDay,
    nextDueEpochDay = nextDueEpochDay,
    isPaused = isPaused,
    createdAtMillis = createdAtMillis
)

private fun BackupRecurringRule.toEntity() = RecurringRuleEntity(
    id = id,
    amountCents = amountCents,
    categoryId = categoryId,
    accountId = accountId,
    merchant = merchant,
    notes = notes,
    cadence = cadence,
    anchorEpochDay = anchorEpochDay,
    nextDueEpochDay = nextDueEpochDay,
    isPaused = isPaused,
    createdAtMillis = createdAtMillis
)
