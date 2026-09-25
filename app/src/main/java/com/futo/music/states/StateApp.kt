package com.futo.music.states

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.futo.music.BuildConfig
import com.futo.music.R
import com.futo.music.RootApplication
import com.futo.music.activities.MainActivity
import com.futo.music.fragments.main.HomeFragment
import com.futo.music.logging.Logger
import com.futo.music.storage.file.FragmentedStorage
import com.futo.music.storage.file.StringStorage
import com.futo.music.storage.file.StringStringMapStorage
import com.futo.music.updater.Updater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.PairSerializer
import java.io.File
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.collections.distinctBy

class StateApp {

    private var _activity: MainActivity? = null;

    var homeRefreshTime: OffsetDateTime = OffsetDateTime.now()
        private set;

    var lastBootVersion: Int? = null;
    var currentBootVersion: Int? = null;


    fun refreshHome() {
        homeRefreshTime = OffsetDateTime.now();
        _activity?.let {
            val cur = it.fragCurrent;
            if(cur is HomeFragment) {
                cur.clearCache();
                scopeOrNull?.launch(Dispatchers.Main) {
                    cur.reloadContent();
                }
            }
        }
    }

    fun registerContext(context: Context) {
        if(context is MainActivity)
            _activity = context;
        FragmentedStorage.initialize(context.applicationContext.filesDir);
    }
    fun activity(): MainActivity? {
        return _activity;
    }


    //Scope
    private var _scope: CoroutineScope? = null;
    val scopeOrNull: CoroutineScope? get() {
        return _scope;
    }
    val scope: CoroutineScope get() {
        val thisScope = scopeOrNull;
        if(thisScope == null) {
            //throw IllegalStateException("Attempted to use a global lifetime scope while MainActivity is no longer available");
            Logger.w(TAG, "Attempted to use a global lifetime scope while MainActivity is no longer available, USING GLOBAL SCOPE");
            return GlobalScope;
        }
        return thisScope;
    }
    val scopeGetter: ()->CoroutineScope get() {
        return {scope};
    }
    fun registerScope(scope: CoroutineScope) {
        _scope = scope;
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun mainAppStarting(context: Context) {
        Logger.i(TAG, "MainApp Starting");

        val shareDir = File(RootApplication.applicationContext.cacheDir, "shares");
        if(shareDir.exists())
            shareDir.deleteRecursively();


        Logger.i(TAG, "MainApp Starting: Initializing [Telemetry]");
        if (!BuildConfig.DEBUG) {
            StateTelemetry.instance.initialize();
            StateTelemetry.instance.upload();
        }

        StatePayment.instance.initialize();

        //Startup tasks
        GlobalScope.launch(Dispatchers.IO) {
            StateFiles.instance.updateFileAccessIds();
            StateFiles.instance.updateRootDirectoryValidation(context);
        };

        val storeLastVersion = FragmentedStorage.get<StringStorage>("lastVersion");
        lastBootVersion = storeLastVersion.value.toIntOrNull();
        val currentBootVersion = BuildConfig.VERSION_CODE;

        if(lastBootVersion != currentBootVersion) {
            GlobalScope.launch(Dispatchers.IO) {
                storeLastVersion.setAndSave(currentBootVersion.toString());

                try {
                    val changelog = Updater.getChangelog(currentBootVersion);
                    if(changelog != null) {
                        StateAnnouncement.instance.registerAnnouncement(Announcement(
                            "update_changelog_" + currentBootVersion.toString(),
                            "Updated to version [${currentBootVersion}]",
                            "You can view the changelog.",
                            AnnouncementType.DELETABLE,
                            OffsetDateTime.now(),
                            actionName = "Changelog",
                            actionId = StateAnnouncement.ACTION_CHANGELOG,
                            actionData = currentBootVersion.toString())
                        )
                    }
                }
                catch(ex: Throwable) {
                    Logger.e(TAG, "Failed to load changelog: " + ex.message, ex);
                }
            }
        }
    }

    fun shareFile(title: String, type: String, file: File) {
        val context = StateApp.instance.activity() ?: return;
        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file);

        val i = Intent(Intent.ACTION_SEND);
        i.type = type;
        i.putExtra(Intent.EXTRA_STREAM, contentUri);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        context.startActivity(Intent.createChooser(i, title));
    }
    fun shareData(title: String, type: String, fileName: String, data: String) {
        val context = StateApp.instance.activity() ?: return;

        val tempFile = getShareFile(fileName);

        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile);

        val i = Intent(Intent.ACTION_SEND);
        i.type = type;
        i.putExtra(Intent.EXTRA_STREAM, contentUri);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        context.startActivity(Intent.createChooser(i, title));
    }

    fun saveFileJson(fileName: String, data: String) {
        activity()?.let {
            it.saveFileJson(fileName) {
                if(it != null)
                    it.write(data);
            }
        }
    }

    fun getShareFile(name: String): File {
        val cacheDir = File(RootApplication.applicationContext.cacheDir, "shares");
        if(!cacheDir.exists())
            cacheDir.mkdir();
        return File(cacheDir, name);
    }

    fun pickFolder(callback: (uri: Uri?)->Unit) {
        activity()?.let {
            it.pickFolder(callback);
        }
    }
    fun pickFile(callback: (uri: Uri?)->Unit, fileTypes: Array<String>? = null) {
        activity()?.let {
            it.pickFile(callback, fileTypes);
        }
    }



    companion object {

        private val TAG = "StateApp";
        @SuppressLint("StaticFieldLeak") //This is only alive while MainActivity is aliv
        private var _instance : StateApp? = null;
        val instance : StateApp
            get(){
                if(_instance == null)
                    _instance = StateApp();
                return _instance!!;
            };

    }
}