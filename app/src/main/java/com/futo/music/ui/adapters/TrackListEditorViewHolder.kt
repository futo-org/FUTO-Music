package com.futo.music.ui.adapters

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
import com.futo.music.storage.db.DBTrack

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

    val onClick = Event1<DBTrack>();
    val onRemove = Event1<DBTrack>();
    val onOptions = Event1<DBTrack>();

    @SuppressLint("ClickableViewAccessibility")
    constructor(view: View, touchHelper: ItemTouchHelper? = null) : super(view) {
        _root = view.findViewById(R.id.root);

        _textName = view.findViewById(R.id.text_name);
        _textMetadata = view.findViewById(R.id.text_metadata);
        _imageThumbnail = view.findViewById(R.id.image_thumbnail);
        _imageDragDrop = view.findViewById(R.id.image_drag_drop);
        _buttonDelete = view.findViewById(R.id.button_delete);
        _buttonMore = view.findViewById(R.id.button_more);


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
    }

    fun bind(t: IPlayableTrack, canEdit: Boolean) {
        if(t is DBTrack) {
            t.getImage()
                ?.setImageView(_imageThumbnail, R.drawable.unknown_music);

            _textName.text = t.name;
            _textMetadata.text = t.author;

            if (canEdit) {
                _buttonDelete.visibility = View.VISIBLE;
                _imageDragDrop.visibility = View.VISIBLE;
            } else {
                _buttonDelete.visibility = View.GONE;
                _imageDragDrop.visibility = View.GONE;
            }

            track = t;
        }
    }
}