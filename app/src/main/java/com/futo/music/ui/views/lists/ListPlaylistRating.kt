package com.futo.music.ui.views.lists

import android.content.Context
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.futo.music.R
import com.futo.music.constructs.Event0
import com.futo.music.constructs.Event1
import com.futo.music.storage.db.DBPlaylist
import com.futo.music.toHumanTimeIndicator
import com.futo.music.ui.buttons.RatingsButton

class ListPlaylistRating: ConstraintLayout {

    private val _imageThumbnail: ImageView;
    private val _textName: TextView;
    //private val _textMetadata: TextView;
    private val _textCount: TextView;
    private val _button: ImageButton;

    private val _rating: RatingsButton;

    val onClick = Event1<DBPlaylist>();

    private var _playlist: DBPlaylist? = null;


    constructor(context: Context) : super(context) {
        inflate(context, R.layout.list_playlist_rating, this);

        _imageThumbnail = findViewById(R.id.image_thumbnail);
        _textName = findViewById(R.id.text_name);
        //_textMetadata = findViewById(R.id.text_metadata);
        _textCount = findViewById(R.id.text_count);
        _button = findViewById(R.id.button);
        _rating = findViewById(R.id.ratings);
        _rating.hideGhost();

        _button.setOnClickListener {
            _playlist?.let {
                onClick.emit(it);
            }
        };
    }

    fun bind(value: DBPlaylist) {
        _playlist = value;
        val img = value.getImage();
        if(img != null)
            img.setImageView(_imageThumbnail, R.drawable.unknown_music);
        else
            _imageThumbnail.setImageResource(R.drawable.unknown_music);

        _textName.text = value.name;
        //_textMetadata.text = if(value.trackDurations > 0) value.trackDurations.toHumanTimeIndicator() else "";

        _rating.setRatingsFor(value);

        if(value.trackCount >= 0) {
            _textCount.text = value.trackCount.toString();
            _textCount.isVisible = true;
        }
        else
            _textCount.isVisible = false;
    }
}