package com.futo.music.activities

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.futo.music.R
import com.futo.music.logging.LogLevel
import com.futo.music.logging.Logging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import kotlin.io.use
import kotlin.jvm.java
import kotlin.text.appendLine
import kotlin.text.replace

class ExceptionActivity : AppCompatActivity() {
    private lateinit var _exText: TextView;
    private lateinit var _buttonShare: LinearLayout;
    //private lateinit var _buttonSubmit: LinearLayout;
    private lateinit var _buttonRestart: LinearLayout;
    private lateinit var _buttonClose: LinearLayout;
    //private lateinit var _buttonCheckForUpdates: LinearLayout;
    private var _file: File? = null;
    private var _submitted = false;

    /*
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(StateApp.instance.getLocaleContext(newBase))
    }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exception);
        //setNavigationBarColorAndIcons();

        class BuildConfigObj {
            val VERSION_NAME: String = ""
            val VERSION_CODE: String = "unknown"
            val FLAVOR: String = "unknown"
            val BUILD_TYPE: String = "unknown"
        }
        val BuildConfig = BuildConfigObj();

        _exText = findViewById(R.id.ex_text);
        _buttonShare = findViewById(R.id.button_share);
        //_buttonSubmit = findViewById(R.id.button_submit);
        _buttonRestart = findViewById(R.id.button_restart);
        _buttonClose = findViewById(R.id.button_close);
        //_buttonCheckForUpdates = findViewById(R.id.button_check_for_updates);

        val context = intent.getStringExtra(EXTRA_CONTEXT) ?: getString(R.string.unknown_context);
        val stack = intent.getStringExtra(EXTRA_STACK) ?: getString(R.string.something_went_wrong_missing_stack_trace);

        val exceptionString = "Version information (version_name = ${BuildConfig.VERSION_NAME}, version_code = ${BuildConfig.VERSION_CODE}, flavor = ${BuildConfig.FLAVOR}, build_type = ${BuildConfig.BUILD_TYPE})\n" +
                "Device information (brand= ${Build.BRAND}, manufacturer = ${Build.MANUFACTURER}, device = ${Build.DEVICE}, version-sdk = ${Build.VERSION.SDK_INT}, version-os = ${Build.VERSION.BASE_OS})\n\n" +
                Logging.buildLogString(LogLevel.ERROR, TAG, "Uncaught exception (\"$context\"): $stack");
        try {
            val file = File(filesDir, "log.txt");
            if (!file.exists()) {
                file.createNewFile();
            }

            BufferedWriter(FileWriter(file, true)).use {
                it.appendLine(exceptionString);
            };

            _file = file
        } catch (e: Throwable) {
            //Ignored
        }

        _exText.text = stack;

        /*
        _buttonSubmit.setOnClickListener {
            submitFile();
        }*/

        _buttonShare.setOnClickListener {
            share(exceptionString);
        };

        _buttonRestart.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java));
        };
        _buttonClose.setOnClickListener {
            finish();
        };

        /*
        if (!BuildConfig.IS_PLAYSTORE_BUILD) {
            _buttonCheckForUpdates.visibility = View.VISIBLE
            _buttonCheckForUpdates.setOnClickListener {
                lifecycleScope.launch(Dispatchers.IO) {
                    StateUpdate.instance.checkForUpdates(this@ExceptionActivity, true, true)
                }
            }
        } else {
            _buttonCheckForUpdates.visibility = View.GONE
        }
        */
    }

    private fun share(exceptionString: String) {
        try {
            val i = Intent(Intent.ACTION_SEND);
            i.type = "text/plain";
            i.putExtra(Intent.EXTRA_EMAIL, arrayOf("grayjay@futo.org"));
            i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.unhandled_exception_in_vs));
            i.putExtra(Intent.EXTRA_TEXT, exceptionString);

            startActivity(Intent.createChooser(i, getString(R.string.send_exception_to_developers)));
        } catch (e: Throwable) {
            //Ignored

        }
    }

    override fun finish() {
        super.finish()
        //overridePendingTransition(R.anim.slide_lighten, R.anim.slide_out_up)
    }

    companion object {
        private const val TAG = "ExceptionActivity";
        val EXTRA_CONTEXT = "CONTEXT";
        val EXTRA_STACK = "STACK";
    }
}