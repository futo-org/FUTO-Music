package com.futo.music.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import com.futo.music.R
import com.futo.music.constructs.Event1
import com.futo.music.formatDuration
import com.futo.music.models.playable.IPlayableTrack
import com.futo.music.storage.db.DBTrack
import com.futo.music.ui.adapters.AnyAdapter

class TrackAnyViewHolder(private val _viewGroup: ViewGroup) : AnyAdapter.AnyViewHolder<IPlayableTrack>(
    LayoutInflater.from(_viewGroup.context).inflate(R.layout.list_track, _viewGroup, false)
) {
    var useFullName: Boolean = false;

    private var _currentPlayable: IPlayableTrack? = null;

    private val _textName = _view.findViewById<TextView>(R.id.text_name);
    private val _textMetadata = _view.findViewById<TextView>(R.id.text_metadata);

    val onClick = Event1<IPlayableTrack>();
    val onLongClick = Event1<IPlayableTrack>();

    init {
        _view.setOnClickListener {
            onClick.emit(_currentPlayable ?: return@setOnClickListener);
        }
        _view.setOnLongClickListener {
            onLongClick.emit(_currentPlayable ?: return@setOnLongClickListener true);
            return@setOnLongClickListener true;
        }
        _view.findViewById<ImageButton>(R.id.button_more).apply {
            setOnClickListener {
                onLongClick.emit(_currentPlayable ?: return@setOnClickListener);
            }
        }
    }

    override fun bind(value: IPlayableTrack) {
        _currentPlayable = value;
        if(value is DBTrack) {
            if(useFullName && !value.artistLine.isNullOrBlank()) {
                _textName.text = value.artistLine!! + " - " + value.name
            }
            else {
                _textName.text = value.name;
            }
            _textMetadata.text = (value.duration * 1000).toLong().formatDuration();
        }
        else {
            _textName.text = "";
            _textMetadata.text = "";
        }
    }
}