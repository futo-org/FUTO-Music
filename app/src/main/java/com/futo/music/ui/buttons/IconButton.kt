package com.futo.music.ui.buttons

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import com.futo.music.R
import com.futo.music.constructs.Event0
import com.futo.music.dp

class IconButton: ConstraintLayout {

    val root: ConstraintLayout;
    val buttonImageContainer: LinearLayout;
    val buttonImage: ImageView;
    val textView: TextView;

    val onClick = Event0();

    constructor(context: Context, attrs: AttributeSet? = null) : super(context, attrs) {
        inflate(context, R.layout.button_icon, this);


        val attrArr = context.obtainStyledAttributes(attrs, R.styleable.IconButton, 0, 0);
        val img = attrArr.getResourceId(R.styleable.IconButton_IconButton_image, 0);
        val color = attrArr.getDrawable(R.styleable.IconButton_IconButton_background);
        val text = attrArr.getString(R.styleable.IconButton_IconButton_text);

        buttonImage = findViewById(R.id.button_image);
        root = findViewById(R.id.root);
        textView = findViewById(R.id.text);
        buttonImageContainer = findViewById(R.id.button_image_container);
        buttonImageContainer.background = color;

        //buttonImage.background = color;
        buttonImage.setImageResource(img);
        buttonImage.setOnClickListener {
            onClick.emit();
        }
        buttonImage.scaleType = ImageView.ScaleType.FIT_CENTER;

        textView.text = text;
    }

}