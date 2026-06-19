package com.futo.music.states

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.futo.music.BuildConfig
import com.futo.music.R
import com.futo.music.RootApplication
import com.futo.music.activities.MainActivity
import com.futo.music.logging.Logger
import com.futo.music.storage.file.FragmentedStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import java.io.File
import java.time.OffsetDateTime

class StateApp {

    private var _activity: MainActivity? = null;

    var homeRefreshTime: OffsetDateTime = OffsetDateTime.now()
        private set;

    fun refreshHome() {
        homeRefreshTime = OffsetDateTime.now();
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

    fun getShareFile(name: String): File {
        val cacheDir = File(RootApplication.applicationContext.cacheDir, "shares");
        if(!cacheDir.exists())
            cacheDir.mkdir();
        return File(cacheDir, name);
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