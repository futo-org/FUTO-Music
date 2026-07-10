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

class StateFiles {


    fun browseFiles() {

    }


    companion object {

        private val TAG = "StateFiles";
        @SuppressLint("StaticFieldLeak") //This is only alive while MainActivity is aliv
        private var _instance : StateFiles? = null;
        val instance : StateFiles
            get(){
                if(_instance == null)
                    _instance = StateFiles();
                return _instance!!;
            };

    }
}