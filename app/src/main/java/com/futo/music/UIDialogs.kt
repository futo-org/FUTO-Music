package com.futo.music

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.net.Uri
import android.provider.Settings
import android.text.Layout
import android.text.method.ScrollingMovementMethod
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.futo.music.logging.Logger
import com.futo.music.states.StateApp
import com.futo.music.ui.views.toasts.ToastView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.collections.toList
import kotlin.let

class UIDialogs {
    companion object {
        private val TAG = "Dialogs"

        private val _openDialogs = arrayListOf<AlertDialog>();

        private fun registerDialogOpened(dialog: AlertDialog) {
            _openDialogs.add(dialog);
        }

        private fun registerDialogClosed(dialog: AlertDialog) {
            _openDialogs.remove(dialog);
        }

        fun dismissAllDialogs() {
            for (openDialog in _openDialogs) {
                openDialog.dismiss();
            }

            _openDialogs.clear();
        }


        fun toast(context : Context, text : String, long : Boolean = false) {
            Toast.makeText(context, text, if(long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show();
        }
        fun toast(text : String, long : Boolean = false) {
            StateApp.instance.scopeOrNull?.launch(Dispatchers.Main) {
                try {
                    RootApplication.applicationContext.let {
                        toast(it, text, long);
                    }
                } catch (e: Throwable) {
                    Logger.e(TAG, "Failed to show toast.", e);
                }
            }
        }
        fun appToast(text: String, long: Boolean = false) {
            appToast(ToastView.Toast(text, long))
        }
        fun appToastError(text: String, long: Boolean) {
            RootApplication.applicationContext.let {
                appToast(ToastView.Toast(text, long, it.getColor(R.color.pastel_red)));
            };
        }
        fun appToast(toast: ToastView.Toast) {
            StateApp.instance.activity()?.let {
                it.showAppToast(toast);
            }
        }
    }

    class Descriptor(val icon: Int, val text: String, val textDetails: String? = null, val code: String? = null, val defaultCloseAction: Int, vararg acts: Action) {
        var shouldShow: ()->Boolean = {true};
        val actions: List<Action> = acts.toList();

        fun withCondition(shouldShow: () -> Boolean): Descriptor {
            this.shouldShow = shouldShow;
            return this;
        }
    }
    class Action {
        val text: String;
        val action: ((DialogResult?)->Unit);
        val style: ActionStyle;
        var center: Boolean;

        constructor(text: String, action: ()->Unit, style: ActionStyle = ActionStyle.NONE, center: Boolean = false) {
            this.text = text;
            this.action = { action() };
            this.style = style;
            this.center = center;
        }
        protected constructor(text: String, action: (DialogResult?)->Unit, style: ActionStyle = ActionStyle.NONE, center: Boolean = false) {
            this.text = text;
            this.action = action;
            this.style = style;
            this.center = center;
        }

        fun invokeAction(input: DialogResult? = null) {
            this.action(input);
        }

        companion object {
            fun withInput(text: String, action: (DialogResult?)->Unit, style: ActionStyle = ActionStyle.NONE, center: Boolean = false): Action {
                return Action(text, action, style, center);
            }
        }
    }
    class DialogResult(
      val text: String?
    );
    enum class ActionStyle {
        NONE,
        PRIMARY,
        ACCENT,
        DANGEROUS,
        DANGEROUS_TEXT
    }
}