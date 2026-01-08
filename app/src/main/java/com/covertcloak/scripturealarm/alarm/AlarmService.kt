package com.covertcloak.scripturealarm.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import com.covertcloak.scripturealarm.R
import com.covertcloak.scripturealarm.data.BibleVerse
import com.covertcloak.scripturealarm.data.BibleVerseRepository
import com.covertcloak.scripturealarm.data.VerseCategory
import com.covertcloak.scripturealarm.ui.AlarmActivity
import java.util.Locale

class AlarmService : Service(), TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var currentVerse: BibleVerse? = null
    private var isTtsReady = false

    companion object {
        const val CHANNEL_ID = "scripture_alarm_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_DISMISS = "com.covertcloak.scripturealarm.DISMISS"
        const val ACTION_SNOOZE = "com.covertcloak.scripturealarm.SNOOZE"

        var currentVerseText: String? = null
        var currentVerseReference: String? = null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        textToSpeech = TextToSpeech(this, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISMISS -> {
                stopAlarm()
                return START_NOT_STICKY
            }
            ACTION_SNOOZE -> {
                snoozeAlarm()
                return START_NOT_STICKY
            }
        }

        val verseCategory = intent?.getStringExtra(AlarmReceiver.EXTRA_VERSE_CATEGORY)?.let {
            try { VerseCategory.valueOf(it) } catch (e: Exception) { VerseCategory.GENERAL }
        } ?: VerseCategory.GENERAL

        val useSequential = intent?.getBooleanExtra(AlarmReceiver.EXTRA_USE_SEQUENTIAL, false) ?: false

        currentVerse = if (useSequential) {
            BibleVerseRepository.getNextVerse()
        } else {
            BibleVerseRepository.getRandomVerse(verseCategory)
        }

        currentVerseText = currentVerse?.text
        currentVerseReference = currentVerse?.reference

        startForeground(NOTIFICATION_ID, createNotification())
        startVibration()
        playAlarmSound()

        // Launch full-screen alarm activity
        val alarmIntent = Intent(this, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("verse_text", currentVerse?.text)
            putExtra("verse_reference", currentVerse?.reference)
        }
        startActivity(alarmIntent)

        return START_STICKY
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.US)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isTtsReady = true
                textToSpeech?.setSpeechRate(0.9f)
                textToSpeech?.setPitch(1.0f)
            }
        }
    }

    fun speakVerse() {
        if (isTtsReady && currentVerse != null) {
            stopAlarmSound()
            stopVibration()

            val textToSpeak = "Good morning. Here is your scripture for today. ${currentVerse!!.reference}. ${currentVerse!!.text}"

            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}

                override fun onDone(utteranceId: String?) {
                    // Speech completed
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {}
            })

            textToSpeech?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "verse_utterance")
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 500, 200, 500, 200, 500, 1000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun stopVibration() {
        vibrator?.cancel()
    }

    private fun playAlarmSound() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmService, alarmUri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAlarmSound() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun snoozeAlarm() {
        // TODO: Implement snooze - reschedule alarm for 5-10 minutes later
        stopAlarm()
    }

    fun stopAlarm() {
        stopVibration()
        stopAlarmSound()
        textToSpeech?.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Scripture Alarm",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Wake up with God's Word"
                setSound(null, null)
                enableVibration(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra("verse_text", currentVerse?.text)
            putExtra("verse_reference", currentVerse?.reference)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_DISMISS
        }
        val dismissPendingIntent = PendingIntent.getService(
            this, 1, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("Scripture Alarm")
            .setContentText(currentVerse?.reference ?: "Time to wake up!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(R.drawable.ic_dismiss, "Dismiss", dismissPendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        stopAlarmSound()
        stopVibration()
        super.onDestroy()
    }
}
