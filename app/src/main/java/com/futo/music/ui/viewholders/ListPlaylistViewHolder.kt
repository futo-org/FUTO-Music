package com.futo.music.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.futo.music.R
import com.futo.music.constructs.Event2
import com.futo.music.storage.db.DBPlaylist
import com.futo.music.toHumanTimeIndicator
import com.futo.music.ui.adapters.AnyAdapter

class ListPlaylistViewHolder(val viewGroup: ViewGroup) : AnyAdapter.AnyViewHolder<ListPlaylistViewHolder.Item>(
    LayoutInflater.from(viewGroup.context).inflate(
        R.layout.list_playlist,
        viewGroup, false
    )) {

    private val _imageThumbnail: ImageView;
    private val _textName: TextView;
    private val _textMetadata: TextView;
    private val _textCount: TextView;
    private val _button: ImageButton;

    val onClick = Event2<ListPlaylistViewHolder, Item>();
    var _playlist: Item? = null;

    init {
        _imageThumbnail = _view.findViewById(R.id.image_thumbnail);
        _textName = _view.findViewById(R.id.text_name);
        _textMetadata = _view.findViewById(R.id.text_metadata);
        _textCount = _view.findViewById(R.id.text_count);
        _button = _view.findViewById(R.id.button);

        _button.setOnClickListener {
            _playlist?.let {
                onClick.emit(this, it);
            }
        };
    }

    override fun bind(value: Item) {
        val img = value.playlist.getImage();
        if(img != null)
            img.setImageView(_imageThumbnail, R.drawable.unknown_music);
        else
            _imageThumbnail.setImageResource(R.drawable.unknown_music);

        _textName.text = value.playlist.name;
        _textMetadata.text = if(value.playlist.trackDurations > 0) value.playlist.trackDurations.toHumanTimeIndicator() else "";

        if(value.playlist.trackCount >= 0) {
            _textCount.text = value.playlist.trackCount.toString();
            _textCount.isVisible = true;
        }
        else
            _textCount.isVisible = false;
        _playlist = value;

        updateAdded();
    }

    fun updateAdded() {
        if(_playlist?.added == true) {
            _button.setImageResource(R.drawable.ic_close);
        }
        else
            _button.setImageResource(R.drawable.ic_playlist_add);
    }


    class Item(var playlist: DBPlaylist, var added: Boolean) {

    }
}