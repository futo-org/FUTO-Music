package com.futo.music.ui.buttons

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.core.view.isVisible
import com.futo.music.R
import com.futo.music.constructs.Event0
import com.futo.music.dp

class PillButton : LinearLayout {
    val root: LinearLayout;
    val icon: ImageView;
    val text: TextView;
    val onClick = Event0();
    private var _isLoading = false;

    constructor(context : Context, attrs : AttributeSet?) : super(context, attrs) {
        LayoutInflater.from(context).inflate(R.layout.pill_button, this, true);
        icon = findViewById(R.id.pill_icon);
        text = findViewById(R.id.pill_text);
        root = findViewById<LinearLayout>(R.id.root);

        val attrArr = context.obtainStyledAttributes(attrs, R.styleable.PillButton, 0, 0);
        val attrIconRef = attrArr.getResourceId(R.styleable.PillButton_pillIcon, -1);
        if(attrIconRef != -1)
            icon.setImageResource(attrIconRef);
        else
            icon.visibility = GONE;

        val attrText = attrArr.getText(R.styleable.PillButton_pillText) ?: "";
        text.text = attrText;

        if(text.text.isNullOrBlank()) {
            val dp6 = 6.dp(resources);
            val dp7 = 7.dp(resources);
            val dp12 = 12.dp(resources);
            root.setPadding(dp7, dp6, dp7, dp7)
        }

        findViewById<LinearLayout>(R.id.root).setOnClickListener {
            if (_isLoading) {
                return@setOnClickListener
            }

            onClick.emit();
        };
    }

    fun setTransparant() {
        root.setBackgroundColor(0);
    }
}