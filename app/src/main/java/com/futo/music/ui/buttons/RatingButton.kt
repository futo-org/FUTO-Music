package com.futo.music.ui.buttons

import android.content.Context
import android.util.AttributeSet
import android.view.View.inflate
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.futo.music.R
import com.futo.music.constructs.Event0

class RatingButton: LinearLayout {

    val onClick = Event0();


    private val stars: List<ImageView>;

    constructor(context: Context, attrs: AttributeSet? = null) : super(context, attrs) {
        val view = inflate(context, R.layout.button_rating, this);

        stars = listOf(
            findViewById(R.id.star_1),
            findViewById(R.id.star_2),
            findViewById(R.id.star_3),
            findViewById(R.id.star_4),
            findViewById(R.id.star_5)
        );

        view.setOnClickListener {
            onClick.emit()
        }

    }

    fun setRating(rating: Int) {
        stars.forEachIndexed { index, view ->
            if(rating >= (index + 1) * 20)
                view.setImageResource(androidx.media3.session.R.drawable.media3_icon_star_filled)
            else
                view.setImageResource(androidx.media3.session.R.drawable.media3_icon_star_unfilled);
        }
    }
}