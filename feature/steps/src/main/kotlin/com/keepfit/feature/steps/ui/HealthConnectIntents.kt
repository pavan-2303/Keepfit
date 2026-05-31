package com.keepfit.feature.steps.ui

import android.content.Context
import android.content.Intent
import android.net.Uri

private const val PROVIDER_PACKAGE_NAME = "com.google.android.apps.healthdata"
private const val ACTION_MANAGE_HEALTH_PERMISSIONS = "android.health.connect.action.MANAGE_HEALTH_PERMISSIONS"

internal fun openHealthConnectStore(context: Context) {
    val marketIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("market://details?id=$PROVIDER_PACKAGE_NAME"),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val webIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://play.google.com/store/apps/details?id=$PROVIDER_PACKAGE_NAME"),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val packageManager = context.packageManager
    val launchIntent = if (marketIntent.resolveActivity(packageManager) != null) marketIntent else webIntent
    context.startActivity(launchIntent)
}

internal fun openHealthConnectPermissions(context: Context) {
    val manageIntent = Intent(ACTION_MANAGE_HEALTH_PERMISSIONS)
        .putExtra(Intent.EXTRA_PACKAGE_NAME, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val packageManager = context.packageManager
    if (manageIntent.resolveActivity(packageManager) != null) {
        context.startActivity(manageIntent)
    } else {
        openHealthConnectStore(context)
    }
}
