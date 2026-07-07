package com.futo.music.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.futo.music.R
import com.futo.music.UIDialogs
import com.futo.music.constructs.Event2
import com.futo.music.models.playable.IPlayable
import com.futo.music.openPlayable
import com.futo.music.states.StateApp
import com.futo.music.states.StateDatabase
import com.futo.music.storage.db.DBDirectory
import com.futo.music.storage.db.DBSetMarkRated
import com.futo.music.storage.db.DBTrack
import com.futo.music.toHumanTime
import com.futo.music.toHumanTimeIndicator
import com.futo.music.ui.adapters.AnyAdapter
import com.futo.music.ui.buttons.RatingsButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DirectoryRootViewHolder(val viewGroup: ViewGroup) : AnyAdapter.AnyViewHolder<DBDirectory>(
    LayoutInflater.from(viewGroup.context).inflate(
        R.layout.list_directory_root,
        viewGroup, false
    )) {

    private val _imageThumbnail: ImageView;
    private val _textName: TextView;
    private val _textMetadata: TextView;
    //private val _textCount: TextView;
    private val _button: ImageButton;

    val onClick = Event2<DirectoryRootViewHolder, DBDirectory>();
    var _playable: DBDirectory? = null;
    private var _markedRated = false;

    init {
        _imageThumbnail = _view.findViewById(R.id.image_thumbnail);
        _textName = _view.findViewById(R.id.text_name);
        _textMetadata = _view.findViewById(R.id.text_metadata);
        _button = _view.findViewById(R.id.button_more);
        _textName.setOnClickListener {
            _playable?.let {
                onClick.emit(this, it);
            }
        };
        _imageThumbnail.setOnClickListener {
            _playable?.let {
                onClick.emit(this, it);
            }
        };
    }

    override fun bind(value: DBDirectory) {
        _textName.text = value.name;
        _textMetadata.text = value.path;

        _playable = value;
    }

}