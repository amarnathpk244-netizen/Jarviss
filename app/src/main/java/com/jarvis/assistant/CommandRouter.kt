package com.jarvis.assistant

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings

/** Offline commands. Returns the spoken reply, or null when the AI brain should handle it. */
object CommandRouter {
    fun route(ctx: Context, raw: String): String? {
        val t = raw.trim().lowercase()
        return try {
            val screenCommand = when (t) {
                "go back", "back", "പിന്നോട്ട്", "തിരികെ" -> "back"
                "go home", "home", "home screen", "ഹോം" -> "home"
                "recent apps", "recents" -> "recents"
                "show notifications", "open notifications", "notifications" -> "notifications"
                "quick settings", "open quick settings" -> "quick settings"
                "scroll down", "താഴേക്ക് സ്ക്രോൾ", "താഴേക്ക്" -> "scroll down"
                "scroll up", "മുകളിലേക്ക് സ്ക്രോൾ", "മുകളിലേക്ക്" -> "scroll up"
                else -> null
            }
            if (screenCommand != null) {
                val service = JarvisAccessibilityService.active
                    ?: return "Enable Jarvis Accessibility in Android Settings first."
                return if (service.command(screenCommand)) "Done." else "I couldn't control this screen."
            }
            Parsing.openTarget(t)?.let { return openApp(ctx, it) }
            Parsing.callTarget(t)?.let { return call(ctx, it) }
            Parsing.navTarget(t)?.let { return navigate(ctx, it) }
            if (t.contains("alarm")) Parsing.alarmTime(t)?.let { (h, m) -> return alarm(ctx, h, m) }
            when {
                t.contains("torch") || t.contains("flashlight") || t.contains("ടോർച്ച്") ->
                    torch(ctx, !(t.contains("off") || t.contains("ഓഫ്")))
                t.contains("battery") || t.contains("ബാറ്ററി") -> battery(ctx)
                t.contains("volume up") || t.contains("louder") -> volume(ctx, true)
                t.contains("volume down") || t.contains("quieter") -> volume(ctx, false)
                t.contains("camera") -> launch(ctx, Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA), "Opening camera.")
                t.contains("settings") -> launch(ctx, Intent(Settings.ACTION_SETTINGS), "Opening settings.")
                else -> null
            }
        } catch (e: SecurityException) {
            "I don't have permission for that."
        } catch (e: Exception) {
            "I couldn't do that."
        }
    }

    private fun launch(ctx: Context, i: Intent, say: String): String {
        ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return say
    }

    private fun openApp(ctx: Context, name: String): String {
        val pm = ctx.packageManager
        val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val match = pm.queryIntentActivities(q, 0).firstOrNull { it.loadLabel(pm).toString().lowercase().contains(name) }
            ?: return "I couldn't find $name."
        val i = pm.getLaunchIntentForPackage(match.activityInfo.packageName) ?: return "I couldn't open $name."
        return launch(ctx, i, "Opening ${match.loadLabel(pm)}.")
    }

    private fun call(ctx: Context, name: String): String {
        val number = ctx.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?", arrayOf("%$name%"), null
        )?.use { if (it.moveToFirst()) it.getString(0) else null } ?: return "I couldn't find $name in your contacts."
        return launch(ctx, Intent(Intent.ACTION_CALL, Uri.parse("tel:" + Uri.encode(number))), "Calling $name.")
    }

    private fun navigate(ctx: Context, dest: String): String =
        launch(
            ctx,
            Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + Uri.encode(dest)))
                .setPackage("com.google.android.apps.maps"),
            "Starting navigation."
        )

    private fun alarm(ctx: Context, h: Int, m: Int): String =
        launch(
            ctx,
            Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_HOUR, h)
                .putExtra(AlarmClock.EXTRA_MINUTES, m).putExtra(AlarmClock.EXTRA_SKIP_UI, true),
            "Alarm set for %d:%02d.".format(h, m)
        )

    private fun torch(ctx: Context, on: Boolean): String {
        val cm = ctx.getSystemService(CameraManager::class.java)
        val id = cm.cameraIdList.firstOrNull {
            cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return "This phone has no flashlight."
        cm.setTorchMode(id, on)
        return if (on) "Flashlight on." else "Flashlight off."
    }

    private fun battery(ctx: Context): String {
        val p = ctx.getSystemService(BatteryManager::class.java).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return "Battery is at $p percent."
    }

    private fun volume(ctx: Context, up: Boolean): String {
        ctx.getSystemService(AudioManager::class.java).adjustStreamVolume(
            AudioManager.STREAM_MUSIC, if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER, 0
        )
        return "Done."
    }
}
