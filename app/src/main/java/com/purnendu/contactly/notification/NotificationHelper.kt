package com.purnendu.contactly.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.purnendu.contactly.R
import kotlin.random.Random

/**
 * Helper class to show fun notifications when alarms trigger
 */
object NotificationHelper {

    private const val CHANNEL_ID = "contactly_alarm_notifications"
    private const val CHANNEL_NAME = "Activation Notifications"
    private const val CHANNEL_DESCRIPTION = "Notifications when contact names are changed"

    // Funny messages for APPLY (name change)
    private val applyMessages = listOf(
        "🎭 Identity swap activated!",
        "🦸 Secret identity engaged!",
        "🎪 Time to put on a mask!",
        "🥷 Ninja mode: ON",
        "🎬 And... ACTION!",
        "🕵️ Undercover operation started!",
        "🦎 Chameleon mode activated!",
        "🎩 *waves magic wand* Alakazam!",
        "🚀 Transformation complete!",
        "🌟 Poof! New name unlocked!",
        "🎯 Mission: Rename initiated!",
        "🤖 Beep boop! Name changed!",
        "🎪 The show begins now!",
        "🦹 Superhero mode: ENGAGED",
        "🎭 Plot twist incoming!"
    )

    // Funny messages for REVERT (name restore)
    private val revertMessages = listOf(
        "🎭 Back to reality!",
        "🦸 Secret identity revealed!",
        "🎪 Mask off time!",
        "🥷 Ninja mode: OFF",
        "🎬 That's a wrap!",
        "🕵️ Mission accomplished!",
        "🦎 Chameleon is tired now!",
        "🎩 *poof* Spell broken!",
        "🚀 Landed back home!",
        "🌟 Original name restored!",
        "🎯 Mission complete!",
        "🤖 Beep boop! Name restored!",
        "🎪 Show's over folks!",
        "🦹 Superhero needs rest!",
        "🎭 Plot twist resolved!"
    )

    /**
     * Create the notification channel (required for Android O+)
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Check if notification permission is granted (Android 13+)
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permission not needed for older Android versions
        }
    }

    /**
     * Show a fun notification when an alarm triggers
     * Note: Caller should check if notifications are enabled before calling this
     */
    fun showAlarmNotification(
        context: Context,
        originalName: String,
        temporaryName: String,
        isApply: Boolean, // true = APPLY (change name), false = REVERT (restore name)
        activationMode: Int, // 0 = ONE_TIME, 1 = REPEAT, 2 = INSTANT, 3 = NEARBY
        contactImage: String?
    ) {
        if (!hasNotificationPermission(context)) return

        // Create channel if not exists
        createNotificationChannel(context)

        // Select random funny message
        val funnyMessage = if (isApply) {
            applyMessages[Random.nextInt(applyMessages.size)]
        } else {
            revertMessages[Random.nextInt(revertMessages.size)]
        }

        // Build notification content
        val activationModeText = when (activationMode) {
            0 -> "One-Time"
            1 -> "Repeat"
            2 -> "Instant"
            3 -> "Nearby"
            else -> "Unknown"
        }
        val actionText = if (isApply) "changed to" else "restored to"

        val content = if (isApply) {
            "\"$originalName\" → \"$temporaryName\" 📝 ($activationModeText)"
        } else {
            "\"$temporaryName\" → \"$originalName\" 📝 ($activationModeText)"
        }

        val expandedText = buildString {
            append("Contact name $actionText: ")
            if (isApply) {
                append("\"$temporaryName\"")
                appendLine()
                append("Original: $originalName")
            } else {
                append("\"$originalName\"")
                appendLine()
                append("Was using: $temporaryName")
            }
            appendLine()
            append("Activation: $activationModeText")
        }

        // Try to load contact image for large icon
        val largeIcon = loadContactImage(contactImage)

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle(funnyMessage)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true) // Dismissible
            .setCategory(NotificationCompat.CATEGORY_STATUS)

        // Add large icon if available
        if (largeIcon != null) {
            notificationBuilder.setLargeIcon(largeIcon)
        }

        val notification = notificationBuilder.build()

        // Generate unique notification ID based on timestamp
        val notificationId = System.currentTimeMillis().toInt()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Permission not granted, silently fail
        }
    }

    /**
     * Load contact image from file path as Bitmap for notification large icon
     */
    private fun loadContactImage(contactImage: String?): Bitmap? {
        if (contactImage.isNullOrBlank()) return null

        return try {
            val file = java.io.File(contactImage)
            if (!file.exists()) {
                Log.w("NotificationHelper", "Image file does not exist: $contactImage")
                return null
            }

            // Decode bitmap with sampling for notification
            val options = BitmapFactory.Options().apply {
                inSampleSize = 2 // Sample by factor of 2 to reduce memory usage
            }

            file.inputStream().use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Failed to load contact image: $contactImage", e)
            null
        }
    }
}
