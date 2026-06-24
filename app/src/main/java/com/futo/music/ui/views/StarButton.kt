package com.futo.music.ui.views

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.futo.music.R
import com.futo.music.models.playable.IPlayable
import com.futo.music.toHumanTimeIndicator
import com.futo.music.toStarRating

class StarButton: ConstraintLayout {

    val image: ImageView;
    val text: TextView;

    constructor(context: Context, attrs: AttributeSet?): super(context, attrs) {
        inflate(context, R.layout.view_star_button, this);

        image = findViewById(R.id.image_star);
        text = findViewById(R.id.text_star);
        setRating(0);
    }

    fun setRatingFor(playable: IPlayable) {
        val rating = playable.score;
        setRating(rating);
    }

    fun setRating(rating: Int = 0) {
        text.text = rating.toStarRating().toString();
        if(rating > 0) {
            image.setImageResource(R.drawable.ic_star_gold);
            text.isVisible = true;
        }
        else {
            image.setImageResource(R.drawable.ic_star_transparent);
            text.isVisible = false;
        }
    }
}