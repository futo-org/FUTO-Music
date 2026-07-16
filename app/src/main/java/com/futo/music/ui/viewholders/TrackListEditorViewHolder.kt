package com.futo.music.ui.viewholders

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.futo.music.R
import com.futo.music.constructs.Event1
import com.futo.music.models.playable.IPlayableTrack
import com.futo.music.states.StateApp
import com.futo.music.storage.db.DBTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrackListEditorViewHolder : ViewHolder {
    private val _root: ConstraintLayout;
    private val _imageDragDrop: ImageButton
    private val _imageThumbnail: ImageView;
    private val _textName: TextView;
    private val _textMetadata: TextView;

    private val _buttonDelete: ImageButton;
    private val _buttonMore: ImageButton;

    var track: DBTrack? = null
        private set;

    private var _isSelected: Boolean = false;
    private var trackCurrent: IPlayableTrack? = null;
    private val trackCurrentChanged: Event1<IPlayableTrack?>?;

    val onClick = Event1<DBTrack>();
    val onRemove = Event1<DBTrack>();
    val onOptions = Event1<DBTrack>();

    @SuppressLint("ClickableViewAccessibility")
    constructor(view: View, touchHelper: ItemTouchHelper? = null, currentTrackChanged: Event1<IPlayableTrack?>? = null, currentTrack: IPlayableTrack? = null) : super(view) {
        _root = view.findViewById(R.id.root);

        _textName = view.findViewById(R.id.text_name);
        _textMetadata = view.findViewById(R.id.text_metadata);
        _imageThumbnail = view.findViewById(R.id.image_thumbnail);
        _imageDragDrop = view.findViewById(R.id.image_drag_drop);
        _buttonDelete = view.findViewById(R.id.button_delete);
        _buttonMore = view.findViewById(R.id.button_more);

        trackCurrent = currentTrack;
        trackCurrentChanged = currentTrackChanged;

        _imageDragDrop.setOnTouchListener { _, event ->
            if (touchHelper != null && event.action == MotionEvent.ACTION_DOWN) {
                touchHelper.startDrag(this);
            }
            false
        };

        _root.setOnClickListener {
            val v = track ?: return@setOnClickListener;
            onClick.emit(v);
        };

        _buttonDelete?.setOnClickListener {
            val v = track ?: return@setOnClickListener;
            onRemove.emit(v);
        };
        _buttonMore?.setOnClickListener {
            val v = track ?: return@setOnClickListener;
            onOptions.emit(v);
        }

        trackCurrentChanged?.subscribe {
            val isSelected = trackCurrent?.getItemId()?.toLongOrNull() == it?.getItemId()?.toLongOrNull();
            trackCurrent = it;
            if(_isSelected != isSelected) {
                _isSelected = isSelected;
                setSelected(_isSelected);
            }
        }
    }

    fun bind(t: IPlayableTrack, canEdit: Boolean, thumbnailVisible: Boolean = false) {
        if(t is DBTrack) {

            _textName.text = t.name;
            _textMetadata.text = t.author;

            if (canEdit) {
                _buttonDelete.visibility = View.VISIBLE;
                _imageDragDrop.visibility = View.VISIBLE;
            } else {
                _buttonDelete.visibility = View.GONE;
                _imageDragDrop.visibility = View.GONE;
            }

            if(thumbnailVisible) {
                StateApp.instance.scopeOrNull?.launch(Dispatchers.IO) {
                    t.getImage().let {
                        withContext(Dispatchers.Main) {
                            if(trackCurrent != t) return@withContext
                            if (it?.isEmpty == false)
                                it.setImageView(_imageThumbnail, R.drawable.unknown_music);
                            else
                                _imageThumbnail.setImageResource(R.drawable.unknown_music);
                            _imageThumbnail.visibility = View.VISIBLE;
                        }
                    }
                }
            }
            else {
                _imageThumbnail.visibility = View.GONE;
            }

            if(t.id == trackCurrent?.getItemId()?.toLongOrNull())
                _isSelected = true;
            else
                _isSelected = false;
            setSelected(_isSelected);

            track = t;
        }
    }

    fun setSelected(selected: Boolean) {
        if(selected)
            _root.setBackgroundColor(android.graphics.Color.argb(0.1f, 1f, 1f, 1f));
        else
            _root.setBackgroundColor(android.graphics.Color.TRANSPARENT);
    }
}