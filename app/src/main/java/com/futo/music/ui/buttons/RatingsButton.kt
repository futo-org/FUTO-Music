package com.futo.music.ui.buttons

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.futo.music.R
import com.futo.music.UIDialogs
import com.futo.music.UIDialogs.Companion.appToast
import com.futo.music.constructs.Event1
import com.futo.music.models.playable.IPlayable
import com.futo.music.models.playable.PlayableType
import com.futo.music.states.StateApp
import com.futo.music.states.StateDatabase
import com.futo.music.storage.db.DBAlbum
import com.futo.music.storage.db.DBArtist
import com.futo.music.storage.db.DBPlaylist
import com.futo.music.storage.db.DBTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RatingsButton: ConstraintLayout {

    private val _container: LinearLayout;
    val buttonStars: List<ImageButton>;

    val onRatingChanged = Event1<Int>()

    var rating: Int = 0
        get() = field;
        private set(v: Int) {
            field = v;
        }

    @SuppressLint("ClickableViewAccessibility")
    constructor(context: Context, attrs: AttributeSet? = null): super(context, attrs) {
        inflate(context, R.layout.button_ratings, this);

        _container = findViewById(R.id.container);
        buttonStars = listOf(
            findViewById<ImageButton>(R.id.button_star_1),
            findViewById<ImageButton>(R.id.button_star_2),
            findViewById<ImageButton>(R.id.button_star_3),
            findViewById<ImageButton>(R.id.button_star_4),
            findViewById<ImageButton>(R.id.button_star_5),
        )

        var ratingBeforeDown = rating;
        _container.setOnTouchListener { view, event ->
            when(event.action) {
                MotionEvent.ACTION_DOWN -> {
                    requestDisallowInterceptTouchEvent(true);
                    ratingBeforeDown = rating;
                    val index = getRatingAtPosition(event.x);
                    if(index != null) {
                        if(rating != index + 1) {
                            setStars(index + 1);
                        }
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    requestDisallowInterceptTouchEvent(true);
                    val index = getRatingAtPosition(event.x);
                    if(index != null) {
                        if(rating != index + 1) {
                            setStars(index + 1);
                        }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    requestDisallowInterceptTouchEvent(true);
                    val index = getRatingAtPosition(event.x);
                    if(index != null) {
                        if(ratingBeforeDown == index + 1) {
                            setStars(ratingBeforeDown - 1);
                        }
                    }
                    if(ratingBeforeDown != rating)
                        onRatingChanged?.emit(rating);
                }
            }
            true
        };
    }

    fun getRatingAtPosition(touchX: Float): Int? {
        for(view in buttonStars) {
            if(touchX > view.left && touchX < view.right)
                return buttonStars.indexOf(view);
        }
        return null;
    }



    private fun setStars(rating: Int) {
        this.rating = rating;
        buttonStars.forEachIndexed { index, button ->
            button.apply {
                if (rating > 0 && rating > index)
                    button.setImageResource(R.drawable.ic_star_gold);
                else
                    button.setImageResource(R.drawable.ic_star_transparent);
            }
        }
    }

    fun setRatingsFor(item: IPlayable?, ratingChanged: ((Int)->Unit)? = null) {
        onRatingChanged.remove(this);
        if(item != null) {
            setStars(item.score / 20);
            onRatingChanged.subscribe {
                val rating = it * 20;
                val currentItem = item;
                ratingChanged?.invoke(rating);
                currentItem.score = rating;
                StateApp.instance.scopeOrNull?.launch(Dispatchers.IO) {
                    val result = if (currentItem is DBAlbum)
                        StateDatabase.instance.setRatingAlbum(currentItem.id, rating);
                    else if (currentItem is DBArtist)
                        StateDatabase.instance.setRatingArtist(currentItem.id, rating);
                    else if (currentItem is DBPlaylist)
                        StateDatabase.instance.setRatingPlaylist(currentItem.id, rating);
                    else if (currentItem is DBTrack)
                        StateDatabase.instance.setRatingTrack(currentItem.id, rating);
                    else false
                    if (!result) {
                        appToast("Failed to update rating");
                    }
                }
            }
        }
        else
            setStars(0);
    }
}