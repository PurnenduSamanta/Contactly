package com.purnendu.contactly.components

import android.content.Context

fun pickTime(context: Context, onPicked: (Long, String) -> Unit) {
    val now = java.util.Calendar.getInstance()
    val dlg = android.app.TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            val cal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
                set(java.util.Calendar.MINUTE, minute)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val millis = cal.timeInMillis
            val label = String.format("%02d:%02d", hourOfDay, minute)
            onPicked(millis, label)
        },
        now.get(java.util.Calendar.HOUR_OF_DAY),
        now.get(java.util.Calendar.MINUTE),
        true
    )
    dlg.show()
}