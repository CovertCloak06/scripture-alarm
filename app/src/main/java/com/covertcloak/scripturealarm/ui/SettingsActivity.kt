package com.covertcloak.scripturealarm.ui

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.covertcloak.scripturealarm.R
import com.covertcloak.scripturealarm.data.AppPreferences
import java.util.Locale

class SettingsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var prefs: AppPreferences
    private var tts: TextToSpeech? = null
    private var availableVoices: List<Voice> = emptyList()

    private lateinit var spinnerVoice: Spinner
    private lateinit var seekBarSpeed: SeekBar
    private lateinit var seekBarPitch: SeekBar
    private lateinit var textSpeedValue: TextView
    private lateinit var textPitchValue: TextView
    private lateinit var btnTestVoice: Button
    private lateinit var radioGroupTheme: RadioGroup
    private lateinit var btnSaveSettings: Button

    private var selectedColorScheme: Int = AppPreferences.COLOR_SCHEME_PURPLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = AppPreferences(this)
        tts = TextToSpeech(this, this)

        setupViews()
        loadSettings()
    }

    private fun setupViews() {
        spinnerVoice = findViewById(R.id.spinnerVoice)
        seekBarSpeed = findViewById(R.id.seekBarSpeed)
        seekBarPitch = findViewById(R.id.seekBarPitch)
        textSpeedValue = findViewById(R.id.textSpeedValue)
        textPitchValue = findViewById(R.id.textPitchValue)
        btnTestVoice = findViewById(R.id.btnTestVoice)
        radioGroupTheme = findViewById(R.id.radioGroupTheme)
        btnSaveSettings = findViewById(R.id.btnSaveSettings)

        // Speed slider (0.5x to 1.5x)
        seekBarSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val speed = 0.5f + (progress / 100f)
                textSpeedValue.text = String.format("%.1fx", speed)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Pitch slider (0.5 to 1.5)
        seekBarPitch.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val pitch = 0.5f + (progress / 100f)
                textPitchValue.text = String.format("%.1f", pitch)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnTestVoice.setOnClickListener {
            testVoice()
        }

        // Color scheme buttons
        findViewById<ImageButton>(R.id.btnColorPurple).setOnClickListener {
            selectedColorScheme = AppPreferences.COLOR_SCHEME_PURPLE
            highlightSelectedColor()
        }
        findViewById<ImageButton>(R.id.btnColorBlue).setOnClickListener {
            selectedColorScheme = AppPreferences.COLOR_SCHEME_BLUE
            highlightSelectedColor()
        }
        findViewById<ImageButton>(R.id.btnColorGreen).setOnClickListener {
            selectedColorScheme = AppPreferences.COLOR_SCHEME_GREEN
            highlightSelectedColor()
        }
        findViewById<ImageButton>(R.id.btnColorOrange).setOnClickListener {
            selectedColorScheme = AppPreferences.COLOR_SCHEME_ORANGE
            highlightSelectedColor()
        }

        btnSaveSettings.setOnClickListener {
            saveSettings()
        }
    }

    private fun loadSettings() {
        // Load speed (convert 0.5-1.5 to 0-100)
        val speedProgress = ((prefs.speechRate - 0.5f) * 100).toInt()
        seekBarSpeed.progress = speedProgress.coerceIn(0, 100)
        textSpeedValue.text = String.format("%.1fx", prefs.speechRate)

        // Load pitch
        val pitchProgress = ((prefs.speechPitch - 0.5f) * 100).toInt()
        seekBarPitch.progress = pitchProgress.coerceIn(0, 100)
        textPitchValue.text = String.format("%.1f", prefs.speechPitch)

        // Load theme
        when (prefs.themeMode) {
            AppPreferences.THEME_SYSTEM -> findViewById<RadioButton>(R.id.radioThemeSystem).isChecked = true
            AppPreferences.THEME_LIGHT -> findViewById<RadioButton>(R.id.radioThemeLight).isChecked = true
            AppPreferences.THEME_DARK -> findViewById<RadioButton>(R.id.radioThemeDark).isChecked = true
        }

        // Load color scheme
        selectedColorScheme = prefs.colorScheme
        highlightSelectedColor()
    }

    private fun highlightSelectedColor() {
        val buttons = listOf(
            R.id.btnColorPurple to AppPreferences.COLOR_SCHEME_PURPLE,
            R.id.btnColorBlue to AppPreferences.COLOR_SCHEME_BLUE,
            R.id.btnColorGreen to AppPreferences.COLOR_SCHEME_GREEN,
            R.id.btnColorOrange to AppPreferences.COLOR_SCHEME_ORANGE
        )

        buttons.forEach { (id, scheme) ->
            val btn = findViewById<ImageButton>(id)
            btn.alpha = if (scheme == selectedColorScheme) 1.0f else 0.5f
            btn.scaleX = if (scheme == selectedColorScheme) 1.2f else 1.0f
            btn.scaleY = if (scheme == selectedColorScheme) 1.2f else 1.0f
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US

            // Get available voices
            availableVoices = tts?.voices?.filter {
                it.locale.language == "en"
            }?.sortedBy { it.name } ?: emptyList()

            val voiceNames = if (availableVoices.isNotEmpty()) {
                availableVoices.map { voice ->
                    val gender = if (voice.name.contains("female", ignoreCase = true)) "Female"
                                 else if (voice.name.contains("male", ignoreCase = true)) "Male"
                                 else ""
                    "${voice.name.substringAfterLast("-")} $gender".trim()
                }
            } else {
                listOf("Default Voice")
            }

            val adapter = ArrayAdapter(this, R.layout.spinner_item, voiceNames)
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
            spinnerVoice.adapter = adapter

            // Select saved voice
            val savedVoiceName = prefs.voiceName
            if (savedVoiceName != null) {
                val index = availableVoices.indexOfFirst { it.name == savedVoiceName }
                if (index >= 0) {
                    spinnerVoice.setSelection(index)
                }
            }
        }
    }

    private fun testVoice() {
        tts?.let { engine ->
            val speed = 0.5f + (seekBarSpeed.progress / 100f)
            val pitch = 0.5f + (seekBarPitch.progress / 100f)

            engine.setSpeechRate(speed)
            engine.setPitch(pitch)

            if (availableVoices.isNotEmpty() && spinnerVoice.selectedItemPosition < availableVoices.size) {
                engine.voice = availableVoices[spinnerVoice.selectedItemPosition]
            }

            engine.speak(
                "The Lord is my shepherd, I shall not want.",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "test"
            )
        }
    }

    private fun saveSettings() {
        // Save voice settings
        prefs.speechRate = 0.5f + (seekBarSpeed.progress / 100f)
        prefs.speechPitch = 0.5f + (seekBarPitch.progress / 100f)

        if (availableVoices.isNotEmpty() && spinnerVoice.selectedItemPosition < availableVoices.size) {
            prefs.voiceName = availableVoices[spinnerVoice.selectedItemPosition].name
        }

        // Save theme
        prefs.themeMode = when (radioGroupTheme.checkedRadioButtonId) {
            R.id.radioThemeSystem -> AppPreferences.THEME_SYSTEM
            R.id.radioThemeLight -> AppPreferences.THEME_LIGHT
            R.id.radioThemeDark -> AppPreferences.THEME_DARK
            else -> AppPreferences.THEME_SYSTEM
        }

        // Apply theme immediately
        when (prefs.themeMode) {
            AppPreferences.THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            AppPreferences.THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        // Save color scheme
        prefs.colorScheme = selectedColorScheme

        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
