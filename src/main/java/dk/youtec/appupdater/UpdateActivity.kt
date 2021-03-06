package dk.youtec.appupdater

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import kotlinx.coroutines.*
import okhttp3.CacheControl
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File
import java.io.IOException
import androidx.lifecycle.lifecycleScope

class UpdateActivity : ComponentActivity() {
    private val tag = UpdateActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val apkUrl = intent.getStringExtra("apkUrl") ?: ""

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            Loader()
        }

        lifecycleScope.launch {
            try {
                downloadApk(this@UpdateActivity, apkUrl)?.also {
                    installApk(this@UpdateActivity, it)
                }
            } catch (e: IOException) {
                Log.e(tag, e.message, e)
                Toast.makeText(
                        this@UpdateActivity,
                        "Unable to update app",
                        Toast.LENGTH_SHORT).show()
            } finally {
                finish()
                overridePendingTransition(0, 0)
            }
        }
    }

    private suspend fun downloadApk(
            context: Context,
            apkUrl: String
    ): File? = withContext(Dispatchers.IO) {
        val httpClient = OkHttpClientFactory.getInstance(context)

        val request = Request.Builder()
                .url(apkUrl)
                .cacheControl(CacheControl.Builder().noStore().build())
                .build()
        val response = httpClient.newCall(request).execute()
        val source = response.body?.source()

        if (source != null) {
            val cacheDir = context.externalCacheDir ?: context.cacheDir
            val apkCacheFolder = File(cacheDir, "apk").apply { mkdirs() }
            val apkFile = File(apkCacheFolder, "app.apk").apply { createNewFile() }
            apkFile.sink().buffer().apply {
                writeAll(source)
                close()
            }

            Log.v(tag,
                    "Downloaded APK to ${apkFile.absolutePath} with size ${apkFile.length()}")

            apkFile
        } else {
            null
        }
    }

    private fun installApk(context: Context, apkFile: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, apkFile)
            val type = "application/vnd.android.package-archive"

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, type)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(tag, e.message, e)
        }
    }
}