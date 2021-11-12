package fast.app.sharer.util

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import fast.app.sharer.R
import fast.app.sharer.model.InstalledAppModel
import fast.app.sharer.receiver.AppSelectorReceiver
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.*
import java.util.*
import java.util.concurrent.TimeUnit


class CustomComparator : Comparator<InstalledAppModel> {
    override fun compare(o1: InstalledAppModel, o2: InstalledAppModel): Int {
        return o1.name!!.compareTo(o2.name!!)
    }
}

open class Util {
    companion object {
        val TAG = "MRT"
        val shareRequestCode = 1001
        val appsFolder = File(Environment.getExternalStorageDirectory().toString() + File.separator + "Apps")
    }

    fun isSystemPackage(packageInfo: PackageInfo): Boolean {
        return if (packageInfo.applicationInfo.flags and 1 == 0 && packageInfo.applicationInfo.flags and 128 == 0) false else true
        //return packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
    }

    fun humanReadableByteCountBin(bytes: Long) = when {
        bytes == Long.MIN_VALUE || bytes < 0 -> "N/A"
        bytes < 1024L -> "$bytes B"
        bytes <= 0xfffccccccccccccL shr 40 -> "%.1f Kb".format(bytes.toDouble() / (0x1 shl 10))
        bytes <= 0xfffccccccccccccL shr 30 -> "%.1f Mb".format(bytes.toDouble() / (0x1 shl 20))
        bytes <= 0xfffccccccccccccL shr 20 -> "%.1f Gb".format(bytes.toDouble() / (0x1 shl 30))
        bytes <= 0xfffccccccccccccL shr 10 -> "%.1f Tb".format(bytes.toDouble() / (0x1 shl 40))
        bytes <= 0xfffccccccccccccL -> "%.1f PiB".format((bytes shr 10).toDouble() / (0x1 shl 40))
        else -> "%.1f EiB".format((bytes shr 20).toDouble() / (0x1 shl 40))
    }

    fun fileCloneAndRename(app: InstalledAppModel): File? {
        val f1 = app.file
        var f2: File? = null

        try {
            f2 = appsFolder
            if (!f2.exists()) {
                f2.mkdir()
            }
            f2 = File(f2.path + File.separator + app.name + "_" + app.versionName + ".apk")
            f2.createNewFile()
        } catch (e: Exception) {
            Log.e(Util.TAG, "122: ${e.message}")
        }

        try {
            val `in`: InputStream = FileInputStream(f1)
            val out: OutputStream = FileOutputStream(f2)
            val buf = ByteArray(4096)
            var len: Int
            while (`in`.read(buf).also { len = it } > 0) {
                out.write(buf, 0, len)
            }
            `in`.close()
            out.close()
            Log.i(TAG, "File copied - ${f1} -> ${f2}")

            return f2
        } catch (e: Exception) {
            Log.e(TAG, "94 : ${e.message}")
            return null
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun shareApp(context: Context, app: InstalledAppModel) {
        try {
            val file: File? = fileCloneAndRename(app)

            val shareIntent = Intent()
            shareIntent.type = "*/*"
            shareIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, "fast.app.sharer.fileprovider", file!!))
            shareIntent.action = Intent.ACTION_SEND

            val receiver = Intent(context, AppSelectorReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, receiver, PendingIntent.FLAG_UPDATE_CURRENT)
            val chooser = Intent.createChooser(shareIntent, context.getString(R.string.share_via), pendingIntent.intentSender)
            (context as Activity).startActivityForResult(chooser, shareRequestCode)

        } catch (e: Exception) {
            Log.e(TAG, "148: ${e.message}")
        }

    }

    fun deleteApk(file: File) {
        if (file.exists()) {
            if (file.delete()) {
                System.out.println("file Deleted :" + file.getPath())
            } else {
                System.out.println("file not Deleted :" + file.getPath())
            }
        }
    }

    fun deleteRecursive(fileOrDirectory: File) {
        if (fileOrDirectory.isDirectory) for (child in fileOrDirectory.listFiles()) deleteRecursive(child)
        fileOrDirectory.delete()
    }

    fun uploadApkToYuklio(activity: Activity, app: InstalledAppModel) {
        StrictMode.setThreadPolicy(ThreadPolicy.Builder().permitAll().build())
        val MEDIA_TYPE_APK = "application/vnd.android.package-archive; charset=utf-8".toMediaType()
        val progressBar = activity.findViewById<ProgressBar>(R.id.prgUploading)
        val progressListener: CountingRequestBody.Listener = object : CountingRequestBody.Listener {
            override fun onRequestProgress(bytesRead: Long, contentLength: Long) {
                if (bytesRead >= contentLength) {
                    if (progressBar != null) activity.runOnUiThread(Runnable { progressBar.setVisibility(View.GONE) })
                } else {
                    if (contentLength > 0) {
                        val progress = (bytesRead.toDouble() / contentLength * 100).toInt()
                        if (progressBar != null) activity.runOnUiThread(Runnable {
                            progressBar.setVisibility(View.VISIBLE)
                            progressBar.setProgress(progress)
                        })
                        if (progress >= 100) {
                            progressBar.setVisibility(View.GONE)
                        }
                        Log.i(TAG, "uploadProgress called: $progress")
                    }
                }
            }
        }

        try {
            val file: File? = fileCloneAndRename(app)

            val client: OkHttpClient = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.MINUTES)
                .writeTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .callTimeout(10, TimeUnit.MINUTES)
                .addNetworkInterceptor(Interceptor { chain ->
                    val originalRequest = chain.request()
                    if (originalRequest.body == null) {
                        return@Interceptor chain.proceed(originalRequest)
                    }
                    val progressRequest = originalRequest.newBuilder()
                        .method(
                            originalRequest.method,
                            CountingRequestBody(originalRequest.body!!, progressListener)
                        )
                        .build()
                    chain.proceed(progressRequest)
                }).build()

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", app.name + ".apk", file!!.asRequestBody(MEDIA_TYPE_APK))
                .build()

            val request = Request.Builder()
                .url("https://yuklio.com/upload.php")
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Call error: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    Log.i(TAG, "Yuklio: ${response.body!!.string()}")

                    activity.runOnUiThread {
                        Runnable {
                            progressBar.visibility = View.VISIBLE
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
        }

    }

    fun hasPermissions(context: Context, vararg permissions: String): Boolean = permissions.all {
        ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}