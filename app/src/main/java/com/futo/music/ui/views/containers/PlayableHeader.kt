package com.futo.music.ui.views.containers

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.marginTop
import com.bumptech.glide.Glide
import com.futo.music.GradientType
import com.futo.music.R
import com.futo.music.colorIntensity
import com.futo.music.constructs.Event0
import com.futo.music.constructs.Event1
import com.futo.music.dp
import com.futo.music.extractBitmap
import com.futo.music.extractColor
import com.futo.music.images.GradientFadeTransformation
import com.futo.music.models.playable.IPlayable
import com.futo.music.states.StateApp
import com.futo.music.storage.db.DBAlbum
import com.futo.music.ui.buttons.RatingsButton
import com.futo.music.ui.views.general.SearchBarView

class PlayableHeader: ConstraintLayout {

    val root: ConstraintLayout;

    val search: SearchBarView;

    val textName: TextView;
    val textMetadata: TextView;
    val imageHeader: ImageView;

    val buttonPlayAll: ImageButton;
    val buttonShuffle: ImageButton;

    var albumCurrent: DBAlbum? = null;

    val buttonRatings: RatingsButton;

    val buttonSwitch: ImageButton;

    val containerViews: LinearLayout;

    val onPlayAll = Event0();
    val onShuffleAll = Event0();
    val onSearchChanged = Event1<String>();
    val onRatingChanged = Event1<Int>();

    private var switchIndex = 0;
    private var switchOptions: List<Pair<Int, ()->Unit>>? = null;

    var expandedImage: Boolean = false;


    constructor(context: Context, attrs: AttributeSet? = null) : super(context, attrs) {
        inflate(context, R.layout.header_playable, this);

        root = findViewById(R.id.root);
        search = findViewById(R.id.view_search);

        textName = findViewById(R.id.text_name);
        textMetadata = findViewById(R.id.text_metadata);
        imageHeader = findViewById(R.id.image_header);

        containerViews = findViewById(R.id.container_additions)

        buttonPlayAll = findViewById(R.id.button_play_all);
        buttonShuffle = findViewById(R.id.button_shuffle);

        buttonRatings = findViewById(R.id.buttons_rating);
        buttonSwitch = findViewById(R.id.button_switch);
        buttonSwitch.isVisible = false;

        search.onChange.subscribe(onSearchChanged::emit);

        buttonPlayAll.setOnClickListener {
            onPlayAll.emit();
        }
        buttonShuffle.setOnClickListener {
            onShuffleAll.emit();
        }

        imageHeader.post {
            imageHeader.pivotX = imageHeader.width / 2f
            imageHeader.pivotY = 0f;
            imageHeader.setOnClickListener {
                if(expandedImage)
                    reduceArt(imageHeader.width);
                else
                    expandArt(imageHeader.width);
            }
        }
    }

    fun expandArt(width: Int) {
        val dp20 = 20.dp(resources);
        val from = textName.marginTop;

        imageHeader.animate()
            .setDuration(500)
            .scaleX(2f)
            .scaleY (2f)
            .start();
        textName.startAnimation(object : Animation() {
            protected override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                val params: LayoutParams = textName.layoutParams as LayoutParams
                params.topMargin = (((dp20 + width) * interpolatedTime) + from * (1f - interpolatedTime)).toInt();
                textName.setLayoutParams(params)
            }
        }.apply { this.duration = 500 })
        expandedImage = true;
    }
    fun reduceArt(width: Int) {
        val dp20 = 20.dp(resources);
        imageHeader.animate()
            .setDuration(500)
            .scaleX(1f)
            .scaleY (1f)
            .start();
        val currentMargin = textName.marginTop;
        textName.startAnimation(object : Animation() {
            protected override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                val params: LayoutParams = textName.layoutParams as LayoutParams
                params.topMargin = ((dp20 * interpolatedTime) + (currentMargin * (1f - interpolatedTime))).toInt();
                textName.setLayoutParams(params)
            }
        }.apply { this.duration = 500 })
        expandedImage = false;
    }

    fun setMetadata(str: String) {
        textMetadata.text = str;
    }

    fun setToggles(options: List<Pair<Int, ()->Unit>>) {
        if(options.size == 0)
            buttonSwitch.isVisible = false;
        else {
            buttonSwitch.setImageResource(options[0].first);
            buttonSwitch.setOnClickListener {
                switchIndex = (switchIndex + 1) % options.size;
                buttonSwitch.setImageResource(options[switchIndex].first);
                options[switchIndex].second.invoke();
            }
            buttonSwitch.isVisible = true;
        }
    }
    fun setToggleIndex(index: Int, trigger: Boolean = false) {
        val options = switchOptions ?: return;
        switchIndex = (index) % options.size;
        buttonSwitch.setImageResource(options[switchIndex].first);
        if(trigger)
            options[index].second.invoke();
    }

    fun setAdditionalViews(views: List<View>) {
        containerViews.removeAllViews();
        for(view in views) {
            containerViews.addView(view);
        }
    }

    fun setPlayable(playable: IPlayable, withGlobalBackground: Boolean = false) {
        buttonRatings.setRatingsFor(playable, { newRating, ghost, type ->
            onRatingChanged?.emit(newRating);
        });

        textName.text = playable.name;

        val image = playable.getImage();
        if(image?.url != null) {
            var builder = Glide.with(imageHeader)
                .load(image.url)
                .fallback(R.drawable.background_button_black);
            if(withGlobalBackground) {
                builder = builder
                    .extractColor { pal ->
                        StateApp.instance.activity()?.let {
                            if (pal != null && (pal.dominant ?: pal.darkVibrant) != null) {
                                var color = (pal.dominant ?: pal.darkVibrant!!);
                                val colorIntensity = color.colorIntensity(100);
                                val intensity = 1f / colorIntensity;
                                if(intensity > 1f)
                                    color = Color.rgb(Color.red(color) * intensity, Color.green(color) * intensity, Color.blue(color) * intensity);
                                it.setBackgroundBottomGradient(
                                    color,
                                    0.5f,
                                    Math.min(1f, intensity)
                                );
                            } else
                                it.hideBackgroundBottom();
                        }
                    }
                    .extractBitmap {
                        if (it == null)
                            return@extractBitmap;
                        StateApp.instance.activity()
                            ?.setBackgroundTop(BitmapDrawable(context.resources, it), 1f, 0.4f, {
                                return@setBackgroundTop it.transform(
                                    GradientFadeTransformation(
                                        GradientType.Vertical,
                                        false
                                    )
                                );
                            })
                    }
            }
            builder.into(imageHeader);
        }
    }
    fun clearSearch(){
        search.clear();
    }
}