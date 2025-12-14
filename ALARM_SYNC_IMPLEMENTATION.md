# Alarm Sync Implementation

## Files Created/Modified

### New Files:
1. **AlarmMetadata.kt** - Data class for alarm metadata
2. **AlarmSyncManager.kt** - Manages sync between AlarmManager and Database
3. **ALARM_SYNC_IMPLEMENTATION.md** - This documentation file

### Modified Files:
1. **ScheduleEntity.kt** - Added `scheduledAlarmsMetadata` field
2. **MainActivityViewModel.kt** - Added alarm sync during splash screen
3. **SchedulesViewModel.kt** - Updated to generate and store alarm metadata
4. **SchedulesRepository.kt** - Added parameter for metadata
5. **RescheduleAlarmsReceiver.kt** - Updated for day selection support

## How It Works

### 1. Database as Single Source of Truth
- All scheduled alarms are tracked in the `schedules` table
- The `scheduledAlarmsMetadata` field stores JSON array of alarm details
- Each AlarmMetadata contains: request code, day of week, operation type, trigger time

### 2. On App Startup (During Splash Screen)
```kotlin
init {
    // 1. Initialize database
    // 2. Sync alarms (this keeps splash screen visible)
    // 3. Mark app as ready (dismisses splash screen)
}
```

### 3. Sync Process
- Reads all schedules from database
- Verifies each contact still exists (orphan cleanup)
- For each stored alarm metadata:
  - Checks if PendingIntent exists in AlarmManager
  - If missing → reschedules it
  - If exists → skips (no duplicate scheduling)

### 4. When Scheduling New Alarms
- App generates AlarmMetadata for each day/operation combination
- Schedules alarms in AlarmManager
- Stores metadata as JSON in database
- Example metadata:
```json
[
  {"requestCode": 102, "dayOfWeek": 1, "operation": "APPLY", "triggerTimeMillis": 1234567890},
  {"requestCode": 103, "dayOfWeek": 1, "operation": "REVERT", "triggerTimeMillis": 1234567900}
]
```

## Dependencies Required

### **IMPORTANT: Add Gson to build.gradle**

Add this to your `app/build.gradle` dependencies:
```gradle
dependencies {
    // Existing dependencies...
    
    // Gson for JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")
}
```

## Database Migration

You mentioned you'll handle the migration. Here's what changed in ScheduleEntity:

**New Column:**
- `scheduledAlarmsMetadata: String?` - Nullable text column for JSON

**Migration SQL:**
```sql
ALTER TABLE schedules ADD COLUMN scheduledAlarmsMetadata TEXT;
```

## Benefits

✅ **Database is single source of truth**
✅ **Detects missing alarms after:**
   - Device reboot
   - Force stop
   - App updates
   - System alarm cleanup
✅ **No duplicate alarms** - checks before scheduling
✅ **Orphaned schedule cleanup** - removes deleted contacts
✅ **Runs during splash screen** - user doesn't notice
✅ **Detailed logging** - easy to debug

## Logging

Check logcat for these tags:
- `AlarmSyncManager` - Sync operations
- `MainActivityViewModel` - Startup sync results
- `SchedulesViewModel` - Alarm scheduling
- `RescheduleAlarmsRx` - Boot receiver operations

## Testing

1. Create a schedule with specific days (e.g., Mon, Wed, Fri)
2. Check database - verify `scheduledAlarmsMetadata` is populated
3. Force stop the app
4. Reopen the app - check logs for sync results
5. Verify alarms are rescheduled (should show "scheduled=X" in logs)
6. Delete a contact, restart app - verify orphaned schedule removed

## Sync Results

On each app startup, MainActivityViewModel logs:
```
Alarm sync completed: 
  scheduled=3 (alarms that were missing and rescheduled)
  skipped=5 (alarms that already existed)
  errors=0 (any sync errors)
  orphaned=1 (schedules removed due to deleted contacts)
```

## Complete Flow

```
User Creates Schedule
       ↓
Generate AlarmMetadata (request codes, days, times)
       ↓
Schedule Alarms in AlarmManager
       ↓
Save to Database (with metadata as JSON)
       ↓
[App Closes / Force Stop / Reboot]
       ↓
App Starts → Splash Screen Visible
       ↓
MainActivityViewModel.syncAlarms()
       ↓
Read all schedules from DB
       ↓
For each schedule:
  - Verify contact exists
  - Parse metadata JSON
  - Check if each alarm exists
  - Reschedule if missing
       ↓
Dismiss Splash Screen
       ↓
App Ready ✓
```

## Important Notes

1. **No Room migration code provided** - You'll handle that
2. **Requires Gson dependency** - Add to build.gradle
3. **PendingIntent equality** - Intents must match exactly for FLAG_NO_CREATE
4. **Request codes** - Uses deterministic scheme based on contactId and day

## Next Steps

1. ✅ Add Gson dependency to build.gradle
2. ✅ Create Room migration for new column
3. ✅ Build and test the app
4. ✅ Check logs to verify sync is working
5. ✅ Test edge cases (force stop, reboot, delete contacts)

---

**Implementation complete! Database is now the single source of truth for scheduled alarms.** 🎉
