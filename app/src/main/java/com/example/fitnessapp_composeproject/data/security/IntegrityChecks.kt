package com.example.fitnessapp_composeproject.data.security

import android.os.Build
import android.os.Debug

object IntegrityChecks {
    fun isDebuggerAttached(): Boolean =
        Debug.isDebuggerConnected() || Debug.waitingForDebugger()

    fun isEmulator(): Boolean =
        Build.FINGERPRINT.contains("generic", ignoreCase = true) ||
                Build.FINGERPRINT.contains("emulator", ignoreCase = true) ||
                Build.MODEL.contains("Emulator", ignoreCase = true)
}