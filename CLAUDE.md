# Scripture Alarm - Development Notes

## Project Overview
Android alarm clock app that reads Bible verses aloud using Text-to-Speech instead of traditional alarm sounds. Gift for wife.

## Current Status (Jan 7, 2025)
**Core features working:**
- Alarm scheduling with day-of-week selection
- Text-to-Speech verse reading on alarm trigger
- Multiple verse categories (Morning, Encouragement, Psalms, Proverbs, Gospels, General)
- Sequential or random verse selection
- Brief vibration before TTS starts
- Full-screen alarm activity with dismiss/snooze

**Settings implemented:**
- Voice selection (friendly names like "Voice 1 (American) HD")
- Speech speed slider (0.5x - 1.5x)
- Voice pitch slider
- Test Voice button
- Theme mode (System/Light/Dark)
- Color schemes (Purple/Blue/Green/Orange)
- Font size options (Small/Medium/Large)

## Known Issues / TODO
1. **Audio routing** - When phone connected via USB/scrcpy, media volume may need manual adjustment
2. **Snooze** - Currently just dismisses alarm (TODO: implement proper snooze with reschedule)
3. **Imagery** - User wants to add category images (paused - waiting for user to provide images)

## Key Files
- `ui/MainActivity.kt` - Main alarm list screen
- `ui/SetAlarmActivity.kt` - Create/edit alarm screen
- `ui/SettingsActivity.kt` - All app settings (voice, theme, colors, fonts)
- `ui/AlarmActivity.kt` - Full-screen alarm display with verse
- `alarm/AlarmService.kt` - Foreground service that handles TTS playback
- `alarm/AlarmScheduler.kt` - Alarm scheduling logic
- `alarm/AlarmReceiver.kt` - BroadcastReceiver for alarm triggers
- `data/AppPreferences.kt` - SharedPreferences wrapper for all settings
- `data/BibleVerseRepository.kt` - Verse database with 35+ verses

## Theme System
Themes use Material Components with color variants:
- Layouts use `?colorPrimary` theme attribute (not hardcoded colors)
- Theme applied in onCreate before setContentView via `setTheme()`
- Requires app restart to see color changes

## Build Commands
```bash
./gradlew assembleDebug
~/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Next Session
- Test audio playback with phone unplugged
- Verify color schemes apply correctly
- Test font size changes
- Add category imagery when user provides images
