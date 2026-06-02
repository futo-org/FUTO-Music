package com.futo.music.ui.buttons

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.futo.music.R
import com.futo.music.constructs.Event0
import com.futo.music.dp

class MenuBottomButton: ConstraintLayout {

    val root: ConstraintLayout;
    val imageView: ImageView;
    val textView: TextView;

    val onClick = Event0();

    var img: Int;
    var imgActive: Int;

    var isActive: Boolean = true
        get() = field
        private set;

    constructor(context: Context, attrs: AttributeSet? = null) : super(context, attrs) {
        inflate(context, R.layout.button_bottom_menu, this);


        val attrArr = context.obtainStyledAttributes(attrs, R.styleable.BottomMenuButton, 0, 0);
        img = attrArr.getResourceId(R.styleable.BottomMenuButton_BottomMenuButton_image, 0);
        imgActive = attrArr.getResourceId(R.styleable.BottomMenuButton_BottomMenuButton_imageActive, 0);
        val text = attrArr.getString(R.styleable.BottomMenuButton_BottomMenuButton_text);

        imageView = findViewById(R.id.image);
        textView = findViewById(R.id.text);
        root = findViewById(R.id.root);

        imageView.setImageResource(img);
        imageView.setOnClickListener {
            onClick.emit();
        }
        root.setOnClickListener {
            onClick.emit()
        }
        textView.setOnClickListener {
            onClick.emit();
        }
        textView.text = text;
        setActive(false);
    }

    fun setActive(active: Boolean) {
        if(active != isActive) {
            isActive = active;
            if(active){
                textView.alpha = 1.0f;
                imageView.alpha = 1.0f;
                imageView.setImageResource(imgActive);
            }
            else {
                textView.alpha = 0.5f;
                imageView.alpha = 0.5f;
                imageView.setImageResource(img);
            }
        }
    }
}