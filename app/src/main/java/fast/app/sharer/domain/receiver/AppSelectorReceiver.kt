package fast.app.sharer.domain.receiver

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import fast.app.sharer.util.TAG
import java.util.*


class AppSelectorReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        Log.i(TAG, "AppSelectorReceiver - context: ${context}")
        Log.i(TAG, "AppSelectorReceiver - intent: ${intent}")

        for (key in Objects.requireNonNull(intent!!.extras)!!.keySet()) {
            try {
                val componentInfo = intent!!.extras!![key] as ComponentName?
                val packageManager = context!!.packageManager
                assert(componentInfo != null)
                val appName = packageManager.getApplicationLabel(packageManager.getApplicationInfo(componentInfo!!.packageName, PackageManager.GET_META_DATA)) as String
                Log.i(TAG, "Selected Application Name: ${appName}")

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}