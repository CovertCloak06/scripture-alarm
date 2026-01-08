package com.covertcloak.scripturealarm.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TimePicker
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import com.covertcloak.scripturealarm.R
import com.covertcloak.scripturealarm.alarm.AlarmScheduler
import com.covertcloak.scripturealarm.data.Alarm
import com.covertcloak.scripturealarm.data.VerseCategory
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.Calendar

class SetAlarmActivity : AppCompatActivity() {

    private lateinit var timePicker: TimePicker
    private lateinit var spinnerCategory: Spinner
    private lateinit var switchSequential: SwitchMaterial
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    private lateinit var btnSunday: ToggleButton
    private lateinit var btnMonday: ToggleButton
    private lateinit var btnTuesday: ToggleButton
    private lateinit var btnWednesday: ToggleButton
    private lateinit var btnThursday: ToggleButton
    private lateinit var btnFriday: ToggleButton
    private lateinit var btnSaturday: ToggleButton

    private var existingAlarmId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_alarm)

        setupViews()
        loadExistingAlarm()
    }

    private fun setupViews() {
        timePicker = findViewById(R.id.timePicker)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        switchSequential = findViewById(R.id.switchSequential)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)

        btnSunday = findViewById(R.id.btnSunday)
        btnMonday = findViewById(R.id.btnMonday)
        btnTuesday = findViewById(R.id.btnTuesday)
        btnWednesday = findViewById(R.id.btnWednesday)
        btnThursday = findViewById(R.id.btnThursday)
        btnFriday = findViewById(R.id.btnFriday)
        btnSaturday = findViewById(R.id.btnSaturday)

        // Setup category spinner
        val categories = VerseCategory.entries.map { category ->
            category.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
        }
        val adapter = ArrayAdapter(this, R.layout.spinner_item, categories)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        btnSave.setOnClickListener {
            saveAlarm()
        }

        btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun loadExistingAlarm() {
        existingAlarmId = intent.getIntExtra("alarm_id", -1)

        if (existingAlarmId > 0) {
            val alarms = AlarmScheduler.getAlarms(this)
            val alarm = alarms.find { it.id == existingAlarmId }

            alarm?.let {
                timePicker.hour = it.hour
                timePicker.minute = it.minute

                val categoryIndex = VerseCategory.entries.indexOf(it.verseCategory)
                if (categoryIndex >= 0) {
                    spinnerCategory.setSelection(categoryIndex)
                }

                switchSequential.isChecked = it.useSequentialVerses

                btnSunday.isChecked = Calendar.SUNDAY in it.daysOfWeek
                btnMonday.isChecked = Calendar.MONDAY in it.daysOfWeek
                btnTuesday.isChecked = Calendar.TUESDAY in it.daysOfWeek
                btnWednesday.isChecked = Calendar.WEDNESDAY in it.daysOfWeek
                btnThursday.isChecked = Calendar.THURSDAY in it.daysOfWeek
                btnFriday.isChecked = Calendar.FRIDAY in it.daysOfWeek
                btnSaturday.isChecked = Calendar.SATURDAY in it.daysOfWeek
            }
        }
    }

    private fun saveAlarm() {
        val hour = timePicker.hour
        val minute = timePicker.minute

        val selectedDays = mutableSetOf<Int>()
        if (btnSunday.isChecked) selectedDays.add(Calendar.SUNDAY)
        if (btnMonday.isChecked) selectedDays.add(Calendar.MONDAY)
        if (btnTuesday.isChecked) selectedDays.add(Calendar.TUESDAY)
        if (btnWednesday.isChecked) selectedDays.add(Calendar.WEDNESDAY)
        if (btnThursday.isChecked) selectedDays.add(Calendar.THURSDAY)
        if (btnFriday.isChecked) selectedDays.add(Calendar.FRIDAY)
        if (btnSaturday.isChecked) selectedDays.add(Calendar.SATURDAY)

        val categoryIndex = spinnerCategory.selectedItemPosition
        val category = VerseCategory.entries[categoryIndex]

        val alarmId = if (existingAlarmId > 0) existingAlarmId else AlarmScheduler.getNextAlarmId(this)

        val alarm = Alarm(
            id = alarmId,
            hour = hour,
            minute = minute,
            enabled = true,
            daysOfWeek = selectedDays,
            verseCategory = category,
            useSequentialVerses = switchSequential.isChecked
        )

        AlarmScheduler.scheduleAlarm(this, alarm)
        Toast.makeText(this, "Alarm set for ${alarm.getTimeString()}", Toast.LENGTH_SHORT).show()
        finish()
    }
}
