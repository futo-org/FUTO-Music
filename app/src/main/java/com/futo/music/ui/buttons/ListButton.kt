package com.futo.music.ui.buttons

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.collection.emptyLongSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.futo.music.R
import com.futo.music.constructs.Event0
import com.futo.music.dp

class ListButton: ConstraintLayout {

    val root: ConstraintLayout;
    val image: ImageView;
    val text: TextView;

    val onClick = Event0();

    constructor(context: Context, attrs: AttributeSet? = null) : super(context, attrs) {
        inflate(context, R.layout.list_button, this);


        val attrArr = context.obtainStyledAttributes(attrs, R.styleable.ListButton, 0, 0);
        val imgAttr = attrArr.getResourceId(R.styleable.ListButton_ListButton_image, 0);
        val textAttr = attrArr.getString(R.styleable.ListButton_ListButton_text);

        image = findViewById(R.id.image);
        text = findViewById(R.id.text);
        root = findViewById(R.id.root);

        image.setImageResource(imgAttr);
        image.setOnClickListener {
            onClick.emit();
        }
        text.setOnClickListener {
            onClick.emit();
        }

        text.text = textAttr;

        if(imgAttr <= 0)
            image.visibility = GONE;
    }

    fun withData(icon: Int, text: String, handler: ()->Unit): ListButton {
        if(icon > 0) {
            this.image.setImageResource(icon);
            this.image.visibility = VISIBLE;
        }
        else
            this.image.visibility = GONE;
        this.text.text = text;
        onClick.subscribe(handler);
        return this;
    }

    fun withMarginBottom(dp: Int): ListButton {
        if(layoutParams != null)
            layoutParams?.let {
                if(it is MarginLayoutParams)
                    it.setMargins(0, 0, 0, dp.dp(resources));
            }
        else {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                this.setMargins(0, 0, 0, dp.dp(resources));
            }
        }
        return this;
    }
}