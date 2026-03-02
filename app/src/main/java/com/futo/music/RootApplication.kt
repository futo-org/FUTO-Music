package com.futo.music

import android.app.Application
import android.content.Context
import com.futo.music.logging.AndroidLogConsumer
import com.futo.music.logging.Logger

class RootApplication : Application() {

    override fun onCreate() {
        Logger.setLogConsumers(listOf(AndroidLogConsumer()));
        _context = applicationContext
        super.onCreate()
    }

    companion object {
        private var _context: Context? = null;
        val applicationContext: Context get() {
                _context.let {
                    if(it == null)
                        throw IllegalStateException("No application context");
                    return it;
                }
            }
    }
}