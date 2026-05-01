package com.freshtrack.notifications

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.freshtrack.data.auth.AuthRepository
import com.freshtrack.data.auth.AuthState
import com.freshtrack.data.db.ProductDao
import com.freshtrack.data.products.toDomain
import com.freshtrack.data.prefs.AppPreferences
import com.freshtrack.domain.model.ProductStatus
import com.freshtrack.domain.model.getEffectiveExpirationDate
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import java.util.concurrent.TimeUnit

@HiltWorker
class ExpirationCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val authRepository: AuthRepository,
    private val productDao: ProductDao,
    private val prefs: AppPreferences
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val notifEnabled = prefs.notifEnabled.first()
        if (!notifEnabled) return Result.success()

        val authState = authRepository.authState.first { it !is AuthState.Loading }
        val householdId = when (val s = authState) {
            is AuthState.Authenticated -> s.household?.id ?: return Result.success()
            else -> return Result.success()
        }

        val notifDays = prefs.notifDays.first()
        val doneIds = prefs.notifDoneIds.first()

        val tz = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toLocalDateTime(tz).date
        val threshold = today.plus(notifDays, DateTimeUnit.DAY)

        val entities = productDao.getByHousehold(householdId)
        val toNotify = entities
            .map { it.toDomain() }
            .filter { it.status == ProductStatus.ACTIVE || it.status == ProductStatus.OPENED }
            .filter { product ->
                val effective = product.getEffectiveExpirationDate()
                effective <= threshold && product.id !in doneIds
            }

        if (toNotify.isEmpty()) {
            prefs.setNotifLastCheck(today.toString())
            return Result.success()
        }

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        val newDoneIds = doneIds.toMutableSet()

        toNotify.forEach { product ->
            val effectiveDate = product.getEffectiveExpirationDate()
            val daysLeft = today.daysUntil(effectiveDate)
            val message = when {
                daysLeft < 0 -> "Périmé depuis ${-daysLeft} jour(s)"
                daysLeft == 0 -> "Expire aujourd'hui !"
                daysLeft == 1 -> "Expire demain"
                else -> "Expire dans $daysLeft jours"
            }

            val notification = NotificationCompat.Builder(applicationContext, NotificationChannels.CHANNEL_EXPIRATION)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(product.name)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setGroup(GROUP_KEY)
                .setAutoCancel(true)
                .build()

            manager.notify(product.id.hashCode(), notification)
            newDoneIds.add(product.id)
        }

        if (toNotify.size > 1) {
            val summary = NotificationCompat.Builder(applicationContext, NotificationChannels.CHANNEL_EXPIRATION)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("${toNotify.size} produits à surveiller")
                .setStyle(NotificationCompat.InboxStyle().setSummaryText("Dates de péremption"))
                .setGroup(GROUP_KEY)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .build()
            manager.notify(SUMMARY_ID, summary)
        }

        prefs.setNotifDoneIds(newDoneIds)
        prefs.setNotifLastCheck(today.toString())
        return Result.success()
    }

    companion object {
        private const val GROUP_KEY = "com.freshtrack.EXPIRATION"
        private const val SUMMARY_ID = 0
        private const val WORK_NAME = "expiration_check"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ExpirationCheckWorker>(24, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
