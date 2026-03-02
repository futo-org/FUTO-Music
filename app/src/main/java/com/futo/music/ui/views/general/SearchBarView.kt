package com.futo.music.ui.views.general

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.inputmethod.InputMethod
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import androidx.compose.ui.platform.ComposeView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.addTextChangedListener
import com.futo.music.R
import com.futo.music.constructs.Event1

class SearchBarView: ConstraintLayout {

    private val _text: EditText;
    private val _root: ConstraintLayout;

    val onFocusChange = Event1<Boolean>();
    val onChange = Event1<String>();

    constructor(context: Context, attrs: AttributeSet? = null): super(context, attrs) {
        inflate(context, R.layout.view_search_bar, this);
        _root = findViewById<ConstraintLayout>(R.id.root);
        _text = findViewById(R.id.text_search);

        findViewById<ImageView>(R.id.button_clear).setOnClickListener {
            _text.text.clear();
            _text.clearFocus();
        }


        val attrArr = context.obtainStyledAttributes(attrs, R.styleable.SearchBarView, 0, 0);
        val attrHint = attrArr.getResourceId(R.styleable.SearchBarView_SearchBarView_hint, 0);
        if(attrHint > 0)
            _text.setHint(attrHint);

        _text.onFocusChangeListener = OnFocusChangeListener({ view, focused ->
            if(focused)
                _root.setBackgroundResource(R.drawable.background_bar_round_active_4dp);
            else
                _root.setBackgroundResource(R.drawable.background_bar_round_4dp);
            onFocusChange.emit(focused);
        });

        _text.addTextChangedListener {
            onChange.emit(it?.toString() ?: "");
        }
    }

    fun focus(inputManager: InputMethodManager? = null) {
        _text.requestFocus();
        if(inputManager != null)
            inputManager.showSoftInput(_text, InputMethodManager.SHOW_IMPLICIT);
    }

    fun setText(str: String) {
        _text.setText(str);
    }
}