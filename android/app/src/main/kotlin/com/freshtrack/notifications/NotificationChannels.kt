package com.freshtrack.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object NotificationChannels {

    const val CHANNEL_EXPIRATION = "expiration"

    fun create(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_EXPIRATION,
            "Dates de péremption",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alertes pour les produits qui approchent de leur date limite"
        }
        manager.createNotificationChannel(channel)
    }
}
