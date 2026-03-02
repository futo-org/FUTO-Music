package com.futo.music.states

import android.annotation.SuppressLint
import android.content.Context
import com.futo.music.activities.MainActivity
import com.futo.music.logging.Logger
import com.futo.music.storage.file.FragmentedStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope

class StateApp {

    private var _activity: MainActivity? = null;

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



    companion object {

        private val TAG = "StateApp";
        @SuppressLint("StaticFieldLeak") //This is only alive while MainActivity is alive
        private var _instance : StateApp? = null;
        val instance : StateApp
            get(){
                if(_instance == null)
                    _instance = StateApp();
                return _instance!!;
            };

    }
}