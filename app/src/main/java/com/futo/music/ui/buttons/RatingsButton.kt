package com.futo.music.ui.buttons

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.futo.music.R
import com.futo.music.UIDialogs
import com.futo.music.UIDialogs.Companion.appToast
import com.futo.music.constructs.Event1
import com.futo.music.models.playable.IPlayable
import com.futo.music.models.playable.PlayableType
import com.futo.music.states.DBPlayableType
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
        private set(v: Int) { field = v; }

    var ratingGhost: Int = 0
        get() = field;
        private set(v: Int) { field = v }

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
            if(touchX > view.left && touchX < view.right) {
                val index = buttonStars.indexOf(view);
                if(index == 0 && touchX < (view.left + (view.width / 7)))
                    return -1;
                return index;
            }
        }
        return null;
    }



    private fun setStars(rating: Int, ghost: Int = 0) {
        this.rating = rating;
        buttonStars.forEachIndexed { index, button ->
            button.apply {
                if (rating > 0 && rating > index)
                    button.setImageResource(R.drawable.ic_star_gold);
                else if(ghost > 0 && ghost > index)
                    button.setImageResource(R.drawable.ic_star_gold_transparant);
                else
                    button.setImageResource(R.drawable.ic_star_transparent);
            }
        }
    }

    private fun getGhostRating(item: IPlayable): Int {
        return if(item is DBTrack && item.scoreLevel != DBPlayableType.Track.value && item.scoreCalculated > 0) item.scoreCalculated / 20 else 0;
    }
    fun setRatingsFor(itemInput: IPlayable?, ratingChanged: ((Int, Int, DBPlayableType?)->Unit)? = null) {
        onRatingChanged.remove(this);
        if(itemInput != null) {
            isVisible = true;
            var item: IPlayable = itemInput;
            val ghost = getGhostRating(item);
            setStars(item.score / 20, ghost);
            onRatingChanged.subscribe {
                val rating = it * 20;
                val currentItem = item;
                currentItem.score = rating;
                ratingChanged?.invoke(rating, getGhostRating(item), DBPlayableType.fromType(item.type));
                StateApp.instance.scopeOrNull?.launch(Dispatchers.IO) {
                    val result = if (currentItem is DBAlbum)
                        StateDatabase.instance.setRatingAlbum(currentItem.id, rating);
                    else if (currentItem is DBArtist)
                        StateDatabase.instance.setRatingArtist(currentItem.id, rating);
                    else if (currentItem is DBPlaylist)
                        StateDatabase.instance.setRatingPlaylist(currentItem.id, rating);
                    else if (currentItem is DBTrack) {
                       val changeResult = StateDatabase.instance.setRatingTrack(currentItem.id, rating);
                        if(changeResult != null) {
                            if(changeResult.first != null && changeResult.first != DBPlayableType.Track) {
                                ratingChanged?.invoke(0, changeResult.second, changeResult.first);
                                if(changeResult.second > 0)
                                    setStars(rating / 20, changeResult.second / 20);
                            }
                        }
                        changeResult != null;
                    }
                    else false
                    if (!result) {
                        appToast("Failed to update rating");
                    }
                }
            }
        }
        else {
            setStars(0);
            isVisible = false;
        }
    }
}