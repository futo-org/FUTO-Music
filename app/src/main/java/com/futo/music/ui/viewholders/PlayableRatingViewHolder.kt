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
import com.futo.music.storage.db.DBSetMarkRated
import com.futo.music.storage.db.DBTrack
import com.futo.music.toHumanTime
import com.futo.music.toHumanTimeIndicator
import com.futo.music.ui.adapters.AnyAdapter
import com.futo.music.ui.buttons.RatingsButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayableRatingViewHolder(val viewGroup: ViewGroup) : AnyAdapter.AnyViewHolder<IPlayable>(
    LayoutInflater.from(viewGroup.context).inflate(
        R.layout.list_playable_rate,
        viewGroup, false
    )) {

    private val _imageThumbnail: ImageView;
    private val _textName: TextView;
    private val _textMetadata: TextView;
    //private val _textCount: TextView;
    private val _button: ImageButton;
    private val _buttonDone: ImageButton;
    private val _ratings: RatingsButton;

    val onClick = Event2<PlayableRatingViewHolder, IPlayable>();
    var _playable: IPlayable? = null;
    private var _markedRated = false;

    init {
        _imageThumbnail = _view.findViewById(R.id.image_thumbnail);
        _textName = _view.findViewById(R.id.text_name);
        _textMetadata = _view.findViewById(R.id.text_metadata);
        //_textCount = _view.findViewById(R.id.text_count);
        _button = _view.findViewById(R.id.button_more);
        _buttonDone = _view.findViewById(R.id.button_done);
        _ratings = _view.findViewById(R.id.ratings);
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
        _buttonDone.setOnClickListener {
            val newValue = !_markedRated;
            _markedRated = newValue;
            _playable?.let {
                if(it is DBTrack) {
                    _buttonDone.setImageResource(if (_markedRated) R.drawable.ic_label_off_active else R.drawable.ic_label_off);
                    if(_markedRated) {
                        _textMetadata.text = "Will disappear on reload";
                    }
                    else
                        _textMetadata.text = "";
                    StateApp.instance.scopeOrNull?.launch(Dispatchers.IO) {
                        StateDatabase.instance.db.tracksDao().setMarkedRated(DBSetMarkRated(it.id, _markedRated));
                    }
                }
            }
        }
    }

    override fun bind(value: IPlayable) {
        /*
        val img = value.playlist.getImage();
        if(img != null)
            img.setImageView(_imageThumbnail, R.drawable.unknown_music);
        else
            _imageThumbnail.setImageResource(R.drawable.unknown_music);
        */

        if(value !is DBTrack) {
            _buttonDone.isVisible = false;
        }
        else {
            _buttonDone.isVisible = true;
            _markedRated = value.markedRated;
            _buttonDone.setImageResource(if(_markedRated) R.drawable.ic_label_off_active else R.drawable.ic_label_off);
        }

        _textName.text = value.name;

        StateApp.instance.scopeOrNull?.launch(Dispatchers.IO) {
            value.getImage().let {
                withContext(Dispatchers.Main) {
                    if(_playable != value) return@withContext
                    if (it?.isEmpty == false)
                        it.setImageView(_imageThumbnail, R.drawable.unknown_music);
                    else
                        _imageThumbnail.setImageResource(R.drawable.unknown_music);
                }
            }
        }

        if(value is DBTrack) {
            _textMetadata.text = value.artistLine//if(value.playlist.trackDurations > 0) value.playlist.trackDurations.toHumanTimeIndicator() else "";
            /*
            if(value.duration >= 0) {
                _textCount.text = value.duration.toLong().toHumanTime(true);
                _textCount.isVisible = true;
            }
            else
                _textCount.isVisible = false;*/
        }
        else {
            _textMetadata.text = "";
            //_textCount.isVisible = false;
        }

        _ratings.setRatingsFor(value);

        _playable = value;


    }

}