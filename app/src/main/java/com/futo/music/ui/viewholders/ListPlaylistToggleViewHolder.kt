package com.futo.music.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.futo.music.R
import com.futo.music.constructs.Event2
import com.futo.music.storage.db.DBPlaylist
import com.futo.music.toHumanTimeIndicator
import com.futo.music.ui.adapters.AnyAdapter
import com.futo.music.ui.views.Checkbox

class ListPlaylistToggleViewHolder(val viewGroup: ViewGroup) : AnyAdapter.AnyViewHolder<ListPlaylistToggleViewHolder.Item>(
    LayoutInflater.from(viewGroup.context).inflate(
        R.layout.list_playlist_toggle,
        viewGroup, false
    )) {

    private val _root: ConstraintLayout;
    private val _imageThumbnail: ImageView;
    private val _textName: TextView;
    private val _textMetadata: TextView;
    private val _textCount: TextView;
    private val _checkbox: Checkbox;

    val onToggleChange = Event2<ListPlaylistToggleViewHolder, Item>();
    var _playlist: Item? = null;

    init {
        _root = _view.findViewById(R.id.root);
        _imageThumbnail = _view.findViewById(R.id.image_thumbnail);
        _textName = _view.findViewById(R.id.text_name);
        _textMetadata = _view.findViewById(R.id.text_metadata);
        _textCount = _view.findViewById(R.id.text_count);
        _checkbox = _view.findViewById(R.id.checkbox);

        _checkbox.onValueChanged.subscribe { v ->
            _playlist?.let {
                it.added = v
                onToggleChange.emit(this@ListPlaylistToggleViewHolder, it);
            }
        }
        _imageThumbnail.setOnClickListener { _checkbox.performClick() };
        _textName.setOnClickListener { _checkbox.performClick() };
        _textMetadata.setOnClickListener { _checkbox.performClick() };
        _root.setOnClickListener { _checkbox.performClick() }
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
        _checkbox.value = _playlist?.added == true;
    }


    class Item(var playlist: DBPlaylist, var added: Boolean) {

    }
}