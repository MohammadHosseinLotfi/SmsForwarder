package com.example.smsforwarder.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.example.smsforwarder.sms.SmsReceiver

object ServicePreferences {
    private const val PREFS_NAME = "sms_forwarder_service_prefs"
    private const val KEY_SERVICE_ENABLED = "is_service_enabled"

    fun isServiceEnabled(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SERVICE_ENABLED, true)
    }

    fun setServiceEnabled(context: Context, enabled: Boolean) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SERVICE_ENABLED, enabled).apply()
        syncReceiverComponentState(appContext, enabled)
    }

    fun syncReceiverComponentState(context: Context, enabled: Boolean) {
        try {
            val appContext = context.applicationContext
            val componentName = ComponentName(appContext, SmsReceiver::class.java)
            val newState = if (enabled) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            appContext.packageManager.setComponentEnabledSetting(
                componentName,
                newState,
                PackageManager.DONT_KILL_APP
            )
        } catch (_: Exception) {
        }
    }
}
