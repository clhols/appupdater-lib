package dk.youtec.appupdater

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import okhttp3.Request
import java.io.IOException
import kotlin.coroutines.coroutineContext

private const val tag = "AppUpdater"

private val json = Json {
    ignoreUnknownKeys = true
}

/**
 * Updates the app to a newer version.
 *
 * @param versionCode BuildConfig.VERSION_CODE of the calling app.
 * @param metaUrl Url pointing to the output.json file from the Gradle build.
 * @param apkUrl Url pointing to the APK file.
 * @param changelogUrl Url pointing to a file with the changelog.
 */
@JvmOverloads
suspend fun Activity.updateApp(
    versionCode: Int,
    metaUrl: String,
    apkUrl: String,
    changelogUrl: String = ""
) {
    if (versionCode == 1) {
        Log.v(tag, "App has debug version code $versionCode")
    } else {
        val metaAppVersion = getAppVersionFromMeta(this, metaUrl)
        Log.v(tag, "Meta has version code $metaAppVersion")

        if (metaAppVersion > versionCode) {
            val message = this.getString(R.string.newAppVersionReady) +
                    "\n\n" +
                    getChangelog(this, changelogUrl).let {
                        it.lines().take(20).run {
                            if (indexOf("") > 0) {
                                dropLastWhile { it.isNotEmpty() }
                            } else {
                                this
                            }
                        }.joinToString("\n")
                    }

            if (coroutineContext.isActive) {
                showUpdateDialog(this, message, apkUrl)
            }
        } else {
            Log.d(tag, "App is the latest version $versionCode")
        }
    }
}

private fun showUpdateDialog(activity: Activity, message: String, apkUrl: String) {
    AlertDialog.Builder(activity)
        .setTitle(activity.getString(R.string.updateApp))
        .setCancelable(true)
        .setMessage(message)
        .setPositiveButton(activity.getString(R.string.update)) { _, _ ->
            activity.startActivity(Intent(activity, UpdateActivity::class.java).apply {
                putExtra("apkUrl", apkUrl)
            })
        }
        .create().show()
}

private suspend fun getAppVersionFromMeta(
    context: Context,
    metaUrl: String
): Long = withContext(Dispatchers.IO) {
    try {
        val httpClient = OkHttpClientFactory.getInstance(context)

        val request = Request.Builder().url(metaUrl).build()
        val response = httpClient.newCall(request).execute()
        val metaString = response.body?.string() ?: ""

        extractVersionCode(metaString)
    } catch (e: Exception) {
        Log.e(tag, e.message ?: "", e)
        -1
    }
}

internal fun extractVersionCode(metaString: String): Long {
    val output = json.decodeFromString(Output.serializer(), metaString)
    return output.elements.firstOrNull()?.versionCode
        ?: throw IllegalStateException("No versionCode found")
}

private suspend fun getChangelog(
    context: Context,
    changelogUrl: String
): String = withContext(Dispatchers.IO) {
    if (changelogUrl.isNotBlank()) {
        try {
            val httpClient = OkHttpClientFactory.getInstance(context)

            val request = Request.Builder().url(changelogUrl).build()
            val response = httpClient.newCall(request).execute()
            response.body?.string() ?: ""
        } catch (e: IOException) {
            Log.e(tag, e.message, e)
            ""
        }
    } else ""
}

private fun Context.getVersionCode(): Long {
    val pInfo: PackageInfo = packageManager.getPackageInfo(packageName, 0)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        pInfo.longVersionCode
    } else {
        pInfo.versionCode.toLong()
    }
}