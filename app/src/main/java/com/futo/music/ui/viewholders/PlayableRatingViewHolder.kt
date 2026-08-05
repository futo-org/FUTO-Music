package com.futo.music.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import com.futo.music.R
import com.futo.music.UIDialogs
import com.futo.music.constructs.Event2
import com.futo.music.constructs.Event3
import com.futo.music.dp
import com.futo.music.logging.Logger
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
    val onRatingChanged = Event3<PlayableRatingViewHolder, IPlayable, Int>();
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
        _ratings.onRatingChanged.subscribe { rating ->
            onRatingChanged.emit(this@PlayableRatingViewHolder, _playable ?: return@subscribe, rating);

            _playable?.let {
                if(it is DBTrack) {
                    setDone(true, rating = it.score);
                    StateApp.instance.scopeOrNull?.launch(Dispatchers.IO) {
                        StateDatabase.instance.db.tracksDao().setMarkedRated(DBSetMarkRated(it.id, _markedRated));
                        it.markedRated = true;
                    }
                }
            }
        }
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
            _playable?.let {
                if(it is DBTrack) {
                    setDone(newValue, rating = it.score);
                    StateApp.instance.scopeOrNull?.launch(Dispatchers.IO) {
                        StateDatabase.instance.db.tracksDao().setMarkedRated(DBSetMarkRated(it.id, _markedRated));
                        it.markedRated = newValue;
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

        Logger.i("PlayableRatingViewHolder", "Setting Rating for " + value.name + ": " + value.score.toString());
        _ratings.setRatingsFor(value);
        _textName.text = value.name;


        if(value is DBTrack)
            _textMetadata.text = value.artistLine
        else
            _textMetadata.text = "";


        if(value !is DBTrack) {
            _buttonDone.isVisible = false;
        }
        else {
            _buttonDone.isVisible = true;
            setDone(value.markedRated || value.score > 0, rating = value.score);
        }

        _playable = value;

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
    }
    fun setDone(isDone: Boolean, byRating: Boolean = false, rating: Int = -999) {
        _markedRated = isDone;
        _buttonDone.setImageResource(if(_markedRated) R.drawable.ic_check_active else R.drawable.ic_check);
        if(_markedRated) {
            _textMetadata.text = "Will disappear on reload";
            _buttonDone.setBackgroundResource(R.drawable.background_bar_round_4dp_22);
            _buttonDone.setPadding(10.dp(_view.context.resources),10.dp(_view.context.resources),10.dp(_view.context.resources),10.dp(_view.context.resources))
            if(rating != -999 && rating <= 0)
                _ratings.alpha = 0.5f;
            else
                _ratings.alpha = 1.0f;
        }
        else {
            _textMetadata.text = "";
            _ratings.alpha = 1f;
            _buttonDone.setBackgroundResource(R.drawable.background_bar_round_4dp);
            _buttonDone.setPadding(10.dp(_view.context.resources),10.dp(_view.context.resources),10.dp(_view.context.resources),10.dp(_view.context.resources))
            _buttonDone.scaleType = ImageView.ScaleType.FIT_CENTER
        }
    }

}