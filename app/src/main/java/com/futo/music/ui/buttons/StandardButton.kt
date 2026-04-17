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

class StandardButton : LinearLayout {
    val root: LinearLayout;
    val icon: ImageView;
    val text: TextView;
    val onClick = Event0();
    private var _isLoading = false;

    constructor(context : Context, attrs : AttributeSet?) : super(context, attrs) {
        LayoutInflater.from(context).inflate(R.layout.button_standard, this, true);
        icon = findViewById(R.id.image_icon);
        text = findViewById(R.id.text_title);
        root = findViewById<LinearLayout>(R.id.root);

        if(attrs != null) {
            val attrArr = context.obtainStyledAttributes(attrs, R.styleable.StandardButton, 0, 0);
            val attrIconRef = attrArr.getResourceId(R.styleable.StandardButton_StandardButton_Icon, -1);
            if (attrIconRef != -1)
                icon.setImageResource(attrIconRef);
            else
                icon.visibility = GONE;

            val attrText = attrArr.getText(R.styleable.StandardButton_StandardButton_Text) ?: "";
            text.text = attrText;

            val attrColor = attrArr.getColor(R.styleable.StandardButton_StandardButton_Color, android.graphics.Color.TRANSPARENT);
            if(attrColor != android.graphics.Color.TRANSPARENT)
                root.setBackgroundColor(attrColor);

            val attrBackground = attrArr.getResourceId(R.styleable.StandardButton_StandardButton_Background, 0);
            if(attrBackground > 0)
                root.setBackgroundResource(attrBackground);
        }

        if(text.text.isNullOrBlank()) {
            val dp6 = 6.dp(resources);
            val dp7 = 7.dp(resources);
            root.setPadding(dp7, dp6, dp7, dp7)
        }

        findViewById<LinearLayout>(R.id.root).setOnClickListener {
            if (_isLoading) {
                return@setOnClickListener
            }

            onClick.emit();
        };
    }

    fun withText(str: String): StandardButton {
        text.text = str;
        if(text.text.isNullOrBlank()) {
        }
        else {
            val dp6 = 6.dp(resources);
            val dp7 = 7.dp(resources);
            val dp12 = 12.dp(resources);
            root.setPadding(dp7, dp6, dp12, dp7)
        }
        return this;
    }
    fun withIcon(resId: Int): StandardButton {
        if(resId != -1)
            icon.setImageResource(resId);
        else
            icon.visibility = GONE;
        return this;
    }

    fun setTransparant() {
        root.setBackgroundColor(0);
    }
}