package com.futo.music.ui.views.playback

import android.content.Context
import android.util.AttributeSet
import android.view.View.inflate
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.futo.music.R
import com.futo.music.UIDialogs
import com.futo.music.constructs.Event0
import com.futo.music.constructs.Event1
import com.futo.music.models.playable.IPlayable
import com.futo.music.models.playable.IPlayableTrack
import com.futo.music.states.StateQueue
import com.futo.music.storage.db.DBTrack
import com.futo.music.ui.views.lists.TrackListEditorView

class QueueOverlay: ConstraintLayout {

    val _root: ConstraintLayout;
    val _textTitle: TextView;
    val _trackEditor: TrackListEditorView;

    val _buttonClose: ImageButton;


    val onTrackClicked = Event1<IPlayableTrack>();
    val onTrackRemoved = Event1<IPlayableTrack>();
    val onTracksOrderChanged = Event1<List<IPlayableTrack>>();
    val onClose = Event0();

    constructor(context: Context, attrs: AttributeSet? = null): super(context, attrs) {
        inflate(context, R.layout.view_queue, this);
        _root = findViewById(R.id.root);

        _textTitle = findViewById(R.id.text_title);
        _trackEditor = findViewById(R.id.track_editor);
        _buttonClose = findViewById(R.id.button_close);

        _buttonClose.setOnClickListener {
            onClose.emit();
        }

        _trackEditor.onTrackClicked.subscribe(onTrackClicked::emit);
        _trackEditor.onTrackRemoved.subscribe(onTrackRemoved::emit);
        _trackEditor.onTrackOrderChanged.subscribe(onTracksOrderChanged::emit);
        _trackEditor.onTrackOptions.subscribe {
            if(it is DBTrack)
                UIDialogs.overlayPlayable(it);
        }
    }

    fun setPlayable(item: IPlayable, tracks: List<IPlayableTrack>) {
        _textTitle.text = item.name;
        _trackEditor.setTracks(tracks, true);
    }

    fun setCurrentTrack(track: IPlayableTrack?) {
        _trackEditor.setCurrentTrack(track);
    }
}