package com.futo.music.ui.views.playback

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.futo.music.PlaySettings
import com.futo.music.R
import com.futo.music.UIDialogs
import com.futo.music.audioContainerToExtension
import com.futo.music.constructs.Event0
import com.futo.music.fragments.main.ArtistFragment
import com.futo.music.fragments.main.PlaybackFragment
import com.futo.music.models.playable.IPlayable
import com.futo.music.models.playable.IPlayableTrack
import com.futo.music.states.StateApp
import com.futo.music.states.StateDatabase
import com.futo.music.states.StateQueue
import com.futo.music.storage.db.DBAlbum
import com.futo.music.storage.db.DBArtist
import com.futo.music.storage.db.DBPlaylist
import com.futo.music.storage.db.DBTrack
import com.futo.music.toSafeFileName
import com.futo.music.ui.buttons.IconButton
import com.futo.music.ui.buttons.ListButton
import com.futo.music.withSettings
import com.futo.music.zipArrays
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.jvm.Throws

class PlayableOptionsView: ConstraintLayout {

    private var _currentPlayable: IPlayable? = null;
    private var _currentPlayableParent: IPlayable? = null;

    private val _root: ConstraintLayout;

    private val _buttonPlay: IconButton;
    private val _buttonPlayNext: IconButton;
    private val _buttonQueueAdd: IconButton;

    private val _buttonPlaylistAdd: ListButton;
    private val _buttonArtist: ListButton;
    private val _buttonRate: ListButton;
    private val _buttonShare: ListButton;

    private val _textTitle: TextView;

    val onHide = Event0();

    private var _showWasInstant = false;

    var isVisible: Boolean = false
        get() {
            return field;
        }
        private set(value) {
            field = value;
        }

    constructor(context: Context, attrs: AttributeSet? = null): super(context, attrs) {
        inflate(context, R.layout.view_playable_options_view, this);
        _root = findViewById<ConstraintLayout>(R.id.root);
        _buttonPlay = findViewById(R.id.button_play);
        _buttonPlayNext = findViewById(R.id.button_play_next);
        _buttonQueueAdd = findViewById(R.id.button_queue_add);
        _textTitle = findViewById(R.id.text_title);


        _buttonPlaylistAdd = findViewById(R.id.button_add_playlist);
        _buttonArtist = findViewById(R.id.button_artist);
        _buttonRate = findViewById(R.id.button_rate);
        _buttonShare = findViewById(R.id.button_share);

        //this.translationY = 1f;
        this.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        //this.alpha = 0f;

        //visibility = INVISIBLE;


        _buttonPlay.onClick.subscribe {
            hide();
            val parent = _currentPlayableParent;
            val current = _currentPlayable;
            val currentItemID = if(current is IPlayableTrack) current.getItemId() else null;
            if(parent != null && currentItemID != null) {
                getScope()?.launch(Dispatchers.IO) {
                    try {
                        val tracks = parent.getTracks(context);
                        val trackIndex = tracks.indexOfFirst { it.getItemId() != null && it.getItemId() == currentItemID};
                        if(trackIndex >= 0) {
                            StateApp.instance.activity()?.navigate<PlaybackFragment>(parent.withSettings(
                                PlaySettings(index = trackIndex)));
                        }
                        else
                            StateApp.instance.activity()?.navigate<PlaybackFragment>(current);
                    }
                    catch(ex: Throwable) {
                        StateApp.instance.activity()?.navigate<PlaybackFragment>(current);
                    }
                }
            }
            else if(current != null)
                StateApp.instance.activity()?.navigate<PlaybackFragment>(current);
        }
        _buttonPlayNext.onClick.subscribe {
            _currentPlayable?.let {
                if(it is DBTrack)
                    StateQueue.instance.setQueuePlayNext(context, it);
            }
            hide();
        }
        _buttonPlaylistAdd.onClick.subscribe {
            _currentPlayable?.let {
                if(it is DBTrack) {
                    UIDialogs.showAddToPlaylistDialog(context, getScope(), "Add to playlist, items are immediately added", it);
                }
                else if(it is DBAlbum) {
                    UIDialogs.showAddToPlaylistDialog(context, getScope(), "Add to playlist, items are immediately added", it);
                }
                else if(it is DBArtist) {
                    UIDialogs.showAddToPlaylistDialog(context, getScope(), "Add to playlist, items are immediately added", it);
                }
            }
            hide();
        }
        _buttonRate.onClick.subscribe {
            _currentPlayable?.let {
                if(it is DBTrack)
                    UIDialogs.showRatingDialog(context, getScope(), it, null, null, null);
                else if(it is DBPlaylist)
                    UIDialogs.showRatingDialog(context, getScope(), null, it, null, null);
                else if(it is DBAlbum)
                    UIDialogs.showRatingDialog(context, getScope(), null, null, it, null);
                else if(it is DBArtist)
                    UIDialogs.showRatingDialog(context, getScope(), null, null, null, it);
            }
            hide();
        }
        _buttonArtist.onClick.subscribe {
            _currentPlayable?.let {
                if(it is DBTrack) {
                    getScope().launch(Dispatchers.IO) {
                        val artist = StateDatabase.instance.getArtist(it.artistId ?: return@launch);
                        withContext(Dispatchers.Main) {
                            StateApp.instance.activity()?.navigate<ArtistFragment>(artist);
                        }
                    }
                }
                else if(it is DBArtist) {
                    StateApp.instance.activity()?.navigate<ArtistFragment>(it);
                }
            }
            hide();
        }
        _buttonShare.onClick.subscribe {
            hide();
            _currentPlayable?.let {
                val fileName = if(it is DBTrack)
                    it.getShareFileName()
                else
                    it.name.toSafeFileName() + ".zip";

                val shareFile = StateApp.instance.getShareFile(fileName);
                shareFile.delete();
                shareFile.createNewFile();
                if(it is DBTrack) {
                    shareFile.outputStream().use { output ->
                        it.getStream(context).use {
                            it?.copyTo(output);
                        }
                    }
                    StateApp.instance.shareFile(fileName, it.mimeType ?: "application/octet-stream", shareFile);
                }
                else {
                    UIDialogs.showDialogProgress(context) { dialog ->
                        dialog.setText("Zipping your files..")
                        val scope = findViewTreeLifecycleOwner()?.lifecycleScope;
                        if(scope == null)
                        {
                            dialog.dismiss();
                            return@showDialogProgress;
                        }
                        scope.launch(Dispatchers.IO) {
                            try {
                                val tracks = it.getTracks(context);
                                shareFile.outputStream().use {
                                    zipArrays(
                                        tracks.filterIsInstance<DBTrack>()
                                            .map { Pair(it.getShareFileName(), it.getStream(context)) }
                                            .filter { it.second != null }
                                            .map { Pair(it.first, it.second!!) }, it, true, { progress, max ->
                                                scope.launch(Dispatchers.Main) {
                                                    dialog.setProgress(progress.toFloat() / max.coerceAtLeast(1));
                                                }
                                        }
                                    );
                                }
                                StateApp.instance.shareFile(fileName, "application/octet-stream", shareFile);
                            }
                            catch(ex: Throwable) {
                                UIDialogs.toast(ex.message ?: "Error in zipping");
                            }
                            finally {
                                dialog.dismiss();
                            }
                        }
                    }
                }
            }
        }
    }

    fun getScope(): CoroutineScope {
        return findViewTreeLifecycleOwner()?.lifecycleScope ?: StateApp.instance.scopeOrNull!!
    }

    fun setPlayable(playable: IPlayable, parentPlayable: IPlayable? = null) {
        _currentPlayable = playable;
        _currentPlayableParent = parentPlayable;

        setButtons(playable, parentPlayable);

        _textTitle.text = playable.name;
        if(_textTitle.text.isEmpty())
            _textTitle.isVisible = false;
        else
            _textTitle.isVisible = true;
    }
    fun hide() {
        onHide.emit();
    }


    fun setButtons(playable: IPlayable, parentPlayable: IPlayable? = null) {
        if(playable is DBTrack) {
            _buttonPlayNext.isVisible = true;
            _buttonQueueAdd.isVisible = true;
        }
        else {
            _buttonPlayNext.isVisible = false;
            _buttonQueueAdd.isVisible = false;
        }

        if(playable is DBTrack || playable is DBPlaylist || playable is DBAlbum || playable is DBArtist)
            _buttonRate.isVisible = true;
        else
            _buttonRate.isVisible = false;

        if(playable is DBTrack || playable is DBArtist || playable is DBAlbum)
            _buttonPlaylistAdd.isVisible = true;
        else
            _buttonPlaylistAdd.isVisible = false;

        if((playable is DBTrack && playable.artistId != null) || playable is DBArtist)
            _buttonArtist.isVisible = true;
        else
            _buttonArtist.isVisible = false;
    }


}