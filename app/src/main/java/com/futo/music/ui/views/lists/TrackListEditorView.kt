package com.futo.music.ui.views.lists

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.futo.music.constructs.Event1
import com.futo.music.models.playable.IPlayableTrack
import com.futo.music.ui.adapters.ItemMoveCallback
import com.futo.music.ui.adapters.TrackListEditorAdapter
import java.util.*
import kotlin.collections.toList
import kotlin.ranges.downTo
import kotlin.ranges.until

class TrackListEditorView : FrameLayout {
    private var _trackCurrent: IPlayableTrack? = null;
    private val _trackCurrentChanged = Event1<IPlayableTrack?>();

    private val _tracks : ArrayList<IPlayableTrack> = ArrayList();

    private var _adapterTracks: TrackListEditorAdapter? = null;

    val onTrackOrderChanged = Event1<List<IPlayableTrack>>()
    val onTrackRemoved = Event1<IPlayableTrack>();
    val onTrackOptions = Event1<IPlayableTrack>();
    val onTrackClicked = Event1<IPlayableTrack>();
    val isEmpty get() = _tracks.isEmpty();
    val itemMoveCallback: ItemMoveCallback

    constructor(context: Context, attrs: AttributeSet? = null) : super(context, attrs) {
        val recyclerPlaylist = RecyclerView(context, attrs);
        recyclerPlaylist.isSaveEnabled = false;

        recyclerPlaylist.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        addView(recyclerPlaylist);

        itemMoveCallback = ItemMoveCallback();
        val touchHelper = ItemTouchHelper(itemMoveCallback);
        val adapterVideos = TrackListEditorAdapter(touchHelper, _trackCurrentChanged, _trackCurrent);
        recyclerPlaylist.adapter = adapterVideos;
        recyclerPlaylist.layoutManager = LinearLayoutManager(context);
        touchHelper.attachToRecyclerView(recyclerPlaylist);

        itemMoveCallback.onRowMoved.subscribe { fromPosition, toPosition ->
            synchronized(_tracks) {
                if (fromPosition < toPosition) {
                    for (i in fromPosition until toPosition)
                        Collections.swap(_tracks, i, i + 1)
                } else {
                    for (i in fromPosition downTo toPosition + 1)
                        Collections.swap(_tracks, i, i - 1)
                }
                onTrackOrderChanged.emit(_tracks.toList());
                adapterVideos.notifyItemMoved(fromPosition, toPosition);
            }
        };

        adapterVideos.onOptions.subscribe { v ->
            onTrackOptions?.emit(v);
        }
        adapterVideos.onRemove.subscribe { v ->
            val executeDelete = {
                synchronized(_tracks) {
                    val index = _tracks.indexOf(v);
                    if (index >= 0) {
                        _tracks.removeAt(index);
                        onTrackRemoved.emit(v);
                    }
                    adapterVideos.notifyItemRemoved(index);
                }
            }

            executeDelete()

        };
        adapterVideos.onClick.subscribe(onTrackClicked::emit);

        _adapterTracks = adapterVideos;
    }

    fun setCurrentTrack(track: IPlayableTrack?) {
        _trackCurrent = track;
        _trackCurrentChanged.emit(track);
    }

    fun setTracks(tracks: List<IPlayableTrack>?, canEdit: Boolean) {
        synchronized(_tracks) {
            _tracks.clear();
            _tracks.addAll(tracks ?: listOf());
            itemMoveCallback.canEdit = canEdit
            _adapterTracks?.setTracks(_tracks, canEdit);
        }
    }

    fun addTracks(tracks: List<IPlayableTrack>) {
        synchronized(_tracks) {
            val index = _tracks.size;
            _tracks.addAll(tracks);
            _adapterTracks?.notifyItemRangeInserted(index, tracks.size);
        }
    }
}