package com.futo.music.ui.buttons

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import com.futo.music.R
import com.futo.music.constructs.Event0
import com.futo.music.dp

class RoundButton: ConstraintLayout {

    val root: ConstraintLayout;
    val buttonImage: ImageButton;

    val onClick = Event0();

    constructor(context: Context, attrs: AttributeSet? = null) : super(context, attrs) {
        inflate(context, R.layout.button_round, this);


        val attrArr = context.obtainStyledAttributes(attrs, R.styleable.RoundButton, 0, 0);
        val img = attrArr.getResourceId(R.styleable.RoundButton_RoundButton_image, 0);
        val color = attrArr.getDrawable(R.styleable.RoundButton_RoundButton_background);

        buttonImage = findViewById(R.id.button_image);
        root = findViewById(R.id.root);
        buttonImage.setImageResource(img);

        buttonImage.setOnClickListener {
            onClick.emit();
        }

        this.background = color;
    }

}