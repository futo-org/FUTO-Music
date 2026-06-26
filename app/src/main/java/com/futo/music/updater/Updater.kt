package com.futo.music.updater

import android.app.PendingIntent.FLAG_MUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.PendingIntent.getBroadcast
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.provider.Settings
import androidx.core.net.toUri
import com.futo.music.R
import com.futo.music.UIDialogs
import com.futo.music.activities.MainActivity
import com.futo.music.api.http.ManagedHttpClient
import com.futo.music.copyToOutputStream
import com.futo.music.logging.Logger
import com.futo.music.models.ImageVariable
import com.futo.music.states.Announcement
import com.futo.music.states.AnnouncementType
import com.futo.music.states.StateAnnouncement
import com.futo.music.states.StateApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.UUID
import kotlin.collections.remove

class Updater {

    val apkUrl: String;
    val versionUrl: String;

    val changelogUrl: String?;

    private val _file: File;
    private var _hasDownload = false;
    private var _downloadVersion = -1;

    private val _client = ManagedHttpClient();

    constructor(apkUrl: String, versionUrl: String, changelogUrl: String?, updateFile: File) {
        this.apkUrl = apkUrl;
        this.versionUrl = versionUrl;
        this.changelogUrl = changelogUrl;
        _file = updateFile;
    }

    fun hasUpdate(currentVersion: Int): Int? {
        val version = getRemoteVersion();
        if(version != null)
            return if(version > currentVersion) version else null;
        return null;
    }

    fun getRemoteVersion(): Int? {
        val result = _client.get(versionUrl, mutableMapOf());
        val respStr = result.body?.string()?.trim();
        val version = respStr?.toIntOrNull();
        return version;
    }

    fun getChangelog(version: Int): String? {
        if(changelogUrl.isNullOrEmpty())
            return null;
        val result = _client.get(changelogUrl.replace("_VERSION_", version.toString()), mutableMapOf());
        val respStr = result.body?.string()?.trim();
        return respStr;
    }

    fun download(onProgress: (Long, Long, Double)->Unit) {
        if(_file.exists())
            _file.delete();

        val version = getRemoteVersion();
        if(version == null)
            throw IllegalStateException("Server not responding with version");

        val resp = _client.get(apkUrl, mutableMapOf());
        if(resp.code < 200 || resp.code >= 400)
            throw IllegalStateException("Bad response (${resp.code})");
        if(resp.body == null)
            throw IllegalStateException("No body response");
        resp.body.byteStream().use { downloadedStr ->
            _file.outputStream().use {
                val buffer = ByteArray(4096);
                var read = downloadedStr.read(buffer);
                var readTotal: Long = 0;
                while(read > 0) {
                    readTotal += read;
                    it.write(buffer, 0, read);
                    read = downloadedStr.read(buffer);
                    onProgress.invoke(resp.body.contentLength(), readTotal, readTotal.toDouble() / resp.body.contentLength());
                }
                onProgress.invoke(resp.body.contentLength(), readTotal, readTotal.toDouble() / resp.body.contentLength());
            }
        }
        _downloadVersion = version;
        _hasDownload = true;
    }

    fun install(context: Context) {
        if(!_hasDownload || _downloadVersion <= 0)
            throw IllegalStateException("No download yet");

        try {
            val pm = context.packageManager
            if (!pm.canRequestPackageInstalls()) {
                UIDialogs.toast(context, "Allow this app to install updates, then try again")
                //UpdateNotificationManager.showInstallFailedNotification(context, _downloadVersion, _file, "Install update permission was missing.")

                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = "package:${context.packageName}".toUri()
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                StateApp.instance.activity()?.requestUnknownInstallUnknownLauncher?.launch(intent);
                return
            }
        } catch (t: Throwable) {
            Logger.e(TAG, "Failed to check unknown sources permission", t)
        }

        GlobalScope.launch(Dispatchers.IO) {
            var inputStream: InputStream? = null
            var session: PackageInstaller.Session? = null
            try {

                val packageInstaller: PackageInstaller = context.packageManager.packageInstaller
                val params =
                    PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
                val sessionId = packageInstaller.createSession(params)
                session = packageInstaller.openSession(sessionId)

                inputStream = _file.inputStream()
                val dataLength = _file.length()

                session.openWrite("package", 0, dataLength).use { sessionStream ->
                    inputStream.copyToOutputStream(dataLength, sessionStream) { _ -> }
                    session.fsync(sessionStream)
                }

                val intent = Intent(context, InstallReceiver::class.java).apply {
                    putExtra("version", _downloadVersion)
                    putExtra("apk_path", _file.absolutePath)
                }
                val pendingIntent =
                    getBroadcast(context, 0, intent, FLAG_MUTABLE or FLAG_UPDATE_CURRENT)
                val statusReceiver = pendingIntent.intentSender

                InstallReceiver.onReceiveResult.subscribe(this) { message ->
                    InstallReceiver.onReceiveResult.clear();
                    onReceiveResult(context, _downloadVersion, _file, message);
                };
                Logger.i(TAG, "Committing install session for ${_file.absolutePath}")
                session.commit(statusReceiver)
            } catch (e: Throwable) {
                Logger.w(TAG, "Exception while installing update", e)
                session?.abandon()
                withContext(Dispatchers.Main) {
                    UIDialogs.toast(context, "Failed to install update: ${e.message}")
                }

                showUpdateFailed(e.message ?: "");
            } finally {
                session?.close()
                inputStream?.close()
            }


        }
    }


    private fun onReceiveResult(context: Context, version: Int, apkFile: File, result: String?) {
        val existingAnnouncement = StateAnnouncement.instance.getVisibleAnnouncements().find { it.id.startsWith("update_installing_") };

        StateApp.instance.scopeOrNull?.launch(Dispatchers.Main) {
            if (existingAnnouncement != null)
                StateAnnouncement.instance.closeAnnouncement(existingAnnouncement.id);

            StateAnnouncement.instance.registerAnnouncement(
                null, "Update Install Failed (v${version})", result ?: "Failed",
                icon = ImageVariable.fromResource(R.mipmap.ic_launcher), actionButton = "Try Again", action = {
                    StateAnnouncement.instance.closeAnnouncement(it.id);
                    StateApp.instance.activity()?.checkForUpdate(true);
                })
        };
        /*
        try {
            InstallReceiver.onReceiveResult.remove(this)

            if (result.isNullOrEmpty()) {
                Logger.i(TAG, "Update install finished successfully")
                UpdateNotificationManager.showInstallSucceededNotification(context, version)
            } else {
                Logger.w(TAG, "Update install failed: $result")
                UpdateNotificationManager.showInstallFailedNotification(context, version, apkFile, result)
                UIDialogs.showGeneralErrorDialog(context, "Install failed due to:\n$result")
            }
        } catch (e: Throwable) {
            Logger.e(com.futo.platformplayer.UpdateInstaller.TAG, "Failed to handle install result", e)
        }
        */
    }


    private fun showUpdateSuccess() {
        StateAnnouncement.instance.registerAnnouncementSession(Announcement("update_success_" + UUID.randomUUID().toString(),
            "Success install", "asdgsadgasd", AnnouncementType.SESSION));
    }
    private fun showUpdateFailed(reason: String) {
        StateAnnouncement.instance.registerAnnouncementSession(Announcement("update_failed_" + UUID.randomUUID().toString(),
            "Failed to install", "${reason}", AnnouncementType.SESSION));
    }


    companion object {
        val TAG = "Updater";
    }
}

