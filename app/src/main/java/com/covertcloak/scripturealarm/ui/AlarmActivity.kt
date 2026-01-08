package com.covertcloak.scripturealarm.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.covertcloak.scripturealarm.R
import com.covertcloak.scripturealarm.alarm.AlarmService
import com.covertcloak.scripturealarm.tts.ScriptureSpeaker

class AlarmActivity : AppCompatActivity() {

    private lateinit var textVerse: TextView
    private lateinit var textReference: TextView
    private lateinit var btnDismiss: Button
    private lateinit var btnReadVerse: Button

    private var scriptureSpeaker: ScriptureSpeaker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Wake up and show on lock screen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        setContentView(R.layout.activity_alarm)

        setupViews()
        displayVerse()
    }

    private fun setupViews() {
        textVerse = findViewById(R.id.textVerse)
        textReference = findViewById(R.id.textReference)
        btnDismiss = findViewById(R.id.btnDismiss)
        btnReadVerse = findViewById(R.id.btnReadVerse)

        btnDismiss.setOnClickListener {
            dismissAlarm()
        }

        btnReadVerse.setOnClickListener {
            readVerseAloud()
        }
    }

    private fun displayVerse() {
        val verseText = intent.getStringExtra("verse_text")
            ?: AlarmService.currentVerseText
            ?: "The Lord is my shepherd; I shall not want."

        val verseReference = intent.getStringExtra("verse_reference")
            ?: AlarmService.currentVerseReference
            ?: "Psalm 23:1"

        textVerse.text = "\"$verseText\""
        textReference.text = "- $verseReference"
    }

    private fun readVerseAloud() {
        // Stop the alarm sound first
        val serviceIntent = Intent(this, AlarmService::class.java)
        bindService(serviceIntent, object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {}
            override fun onServiceDisconnected(name: ComponentName?) {}
        }, Context.BIND_AUTO_CREATE)

        val verseText = textVerse.text.toString().trim('"')
        val verseReference = textReference.text.toString().removePrefix("- ")

        scriptureSpeaker = ScriptureSpeaker(
            this,
            onReady = {
                val verse = com.covertcloak.scripturealarm.data.BibleVerse(
                    book = "",
                    chapter = 0,
                    verse = 0,
                    text = verseText
                )
                scriptureSpeaker?.speakVerse(verse, includeGreeting = true)
            },
            onComplete = {
                runOnUiThread {
                    btnReadVerse.text = "Read Again"
                }
            }
        )

        btnReadVerse.text = "Reading..."
        btnReadVerse.isEnabled = false

        // Re-enable after a delay
        btnReadVerse.postDelayed({
            btnReadVerse.isEnabled = true
        }, 1000)
    }

    private fun dismissAlarm() {
        scriptureSpeaker?.shutdown()

        // Stop the alarm service
        val intent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_DISMISS
        }
        startService(intent)

        finish()
    }

    override fun onDestroy() {
        scriptureSpeaker?.shutdown()
        super.onDestroy()
    }

    override fun onBackPressed() {
        // Don't allow back button to dismiss alarm easily
        // User must tap the dismiss button
    }
}
