package com.futo.music.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.futo.music.R
import com.futo.music.constructs.Event1
import com.futo.music.models.playable.IPlayableTrack
import com.futo.music.storage.db.DBTrack
import kotlin.let

class TrackListEditorAdapter : RecyclerView.Adapter<TrackListEditorViewHolder> {
    private var _tracks: ArrayList<IPlayableTrack>? = null;
    private val _touchHelper: ItemTouchHelper;

    val onClick = Event1<DBTrack>();
    val onRemove = Event1<DBTrack>();
    val onOptions = Event1<DBTrack>();
    var canEdit = false
        private set;

    private var _trackCurrent: IPlayableTrack? = null;
    private val _trackCurrentChanged: Event1<IPlayableTrack?>;

    constructor(touchHelper: ItemTouchHelper, trackChanged: Event1<IPlayableTrack?>, trackCurrent: IPlayableTrack? = null) : super() {
        _touchHelper = touchHelper;
        _trackCurrentChanged  = trackChanged;
        trackChanged.subscribe {
            _trackCurrent = it;
        }
    }

    override fun getItemCount() = _tracks?.size ?: 0;

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): TrackListEditorViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.list_track_editable, viewGroup, false);
        val holder = TrackListEditorViewHolder(view, _touchHelper, _trackCurrentChanged, _trackCurrent);

        holder.onRemove.subscribe { v -> onRemove.emit(v); };
        holder.onOptions.subscribe { v -> onOptions.emit(v); };
        holder.onClick.subscribe { v -> onClick.emit(v); };

        return holder;
    }

    override fun onBindViewHolder(viewHolder: TrackListEditorViewHolder, position: Int) {
        val tracks = _tracks ?: return;
        viewHolder.bind(tracks[position], canEdit);
    }

    fun setCanEdit(canEdit: Boolean, notify: Boolean = false) {
        this.canEdit = canEdit;
        if (notify) {
            _tracks?.let { notifyItemRangeChanged(0, it.size); };
        }
    }

    fun setTracks(tracks: ArrayList<IPlayableTrack>, canEdit: Boolean) {
        _tracks = tracks;
        setCanEdit(canEdit, false);
        notifyDataSetChanged();
    }
}
