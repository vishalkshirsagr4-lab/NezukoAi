package com.nezuko.ai.phone

import android.content.Context
import android.content.Intent
import android.net.Uri

class AndroidActions(private val context: Context) {
    fun openHttpsUrl(raw: String): Result<Unit> = runCatching {
        val uri = Uri.parse(raw.trim())
        require(uri.scheme == "https" && !uri.host.isNullOrBlank()) { "Only valid HTTPS links can be opened." }
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    fun openDialer(number: String): Result<Unit> = runCatching {
        require(number.matches(Regex("[+0-9() .-]{3,24}"))) { "That phone number format is not allowed." }
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number)}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    fun openApp(packageName: String): Result<Unit> = runCatching {
        require(packageName.matches(Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+"))) { "Invalid app package." }
        val launch = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: throw IllegalArgumentException("That app is not installed or cannot be launched.")
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launch)
    }
    fun openSettings(): Result<Unit> = runCatching {
        context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
