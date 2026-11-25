package com.saurav.pixelmusic.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import com.saurav.pixelmusic.R

enum class AppIconOption(
    val id: String,
    val label: String,
    val aliasClassName: String,
    @DrawableRes val previewRes: Int?,
    val isAdaptiveZoomPreview: Boolean = true
) {
    DEFAULT(
        id = "default",
        label = "Default",
        aliasClassName = "com.saurav.pixelmusic.launcher.DefaultLauncher",
        previewRes = null,
        isAdaptiveZoomPreview = false
    ),
    PEACH_GLASS(
        id = "peach_glass",
        label = "Peach Glass",
        aliasClassName = "com.saurav.pixelmusic.launcher.PeachGlassLauncher",
        previewRes = R.drawable.ic_launcher_custom_peach_glass
    ),
    BERRY_PULSE(
        id = "berry_pulse",
        label = "Berry Pulse",
        aliasClassName = "com.saurav.pixelmusic.launcher.BerryPulseLauncher",
        previewRes = R.drawable.ic_launcher_custom_berry_pulse
    ),
    SAGE_WAVE(
        id = "sage_wave",
        label = "Sage Wave",
        aliasClassName = "com.saurav.pixelmusic.launcher.SageWaveLauncher",
        previewRes = R.drawable.ic_launcher_custom_sage_wave
    ),
    SKY_CLOUD(
        id = "sky_cloud",
        label = "Sky Cloud",
        aliasClassName = "com.saurav.pixelmusic.launcher.SkyCloudLauncher",
        previewRes = R.drawable.ic_launcher_custom_sky_cloud
    ),
    WARM_MINIMAL(
        id = "warm_minimal",
        label = "Warm Minimal",
        aliasClassName = "com.saurav.pixelmusic.launcher.WarmMinimalLauncher",
        previewRes = R.drawable.ic_launcher_custom_warm_minimal
    ),
    EMERALD_INK(
        id = "emerald_ink",
        label = "Emerald Ink",
        aliasClassName = "com.saurav.pixelmusic.launcher.EmeraldInkLauncher",
        previewRes = R.drawable.ic_launcher_custom_emerald_ink
    ),
    WOVEN_FABRIC(
        id = "woven_fabric",
        label = "Woven Fabric",
        aliasClassName = "com.saurav.pixelmusic.launcher.WovenFabricLauncher",
        previewRes = R.drawable.ic_launcher_custom_woven_fabric
    ),
    COZY_KNIT(
        id = "cozy_knit",
        label = "Cozy Knit",
        aliasClassName = "com.saurav.pixelmusic.launcher.CozyKnitLauncher",
        previewRes = R.drawable.ic_launcher_custom_cozy_knit
    )
}

object AppIconManager {

    fun getCurrentIcon(context: Context): AppIconOption {
        val pm = context.packageManager
        for (option in AppIconOption.entries) {
            if (option == AppIconOption.DEFAULT) continue
            val state = pm.getComponentEnabledSetting(
                ComponentName(context.packageName, option.aliasClassName)
            )
            if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                return option
            }
        }
        return AppIconOption.DEFAULT
    }

    fun applyIcon(context: Context, target: AppIconOption) {
        val pm = context.packageManager
        // Enable the target alias first so there is always an active launcher entry
        pm.setComponentEnabledSetting(
            ComponentName(context.packageName, target.aliasClassName),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        for (option in AppIconOption.entries) {
            if (option == target) continue
            pm.setComponentEnabledSetting(
                ComponentName(context.packageName, option.aliasClassName),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}
