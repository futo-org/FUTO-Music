package com.futo.music.fragments.main

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.TimeBar
import com.bumptech.glide.Glide
import com.bumptech.glide.TransitionOptions
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.futo.music.IPlayableWithPlaySettings
import com.futo.music.R
import com.futo.music.UIDialogs
import com.futo.music.extensions.setAlbumArt
import com.futo.music.fragments.MainFragView
import com.futo.music.logic.PlayerManager
import com.futo.music.models.playable.IPlayable
import com.futo.music.models.playable.IPlayableTrack
import com.futo.music.services.PlaybackService
import com.futo.music.states.StateDatabase
import com.futo.music.states.StateQueue
import com.futo.music.storage.db.DBAlbum
import com.futo.music.storage.db.DBArtist
import com.futo.music.storage.db.DBPlaylist
import com.futo.music.storage.db.DBTrack
import com.futo.music.ui.buttons.RatingButton
import com.futo.music.ui.views.playback.QueueOverlay
import com.google.android.material.imageview.ShapeableImageView
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaybackFragment: MainFragment() {
    override val isMainView : Boolean = true;
    override val isTab: Boolean = true;

    private var _player: PlayerManager? = null;

    private var _view: FragView? = null;

    fun setPlayer(player: PlayerManager) {
        _player = player;
        _view?.onPlayerAvailable(player);
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onShownWithView(parameter: Any?, isBack: Boolean) {
        super.onShownWithView(parameter, isBack);
        _view?.onShown(parameter);
    }

    override fun onHide() {
        super.onHide();
        _view?.onHide();
    }

    override fun onCreateMainView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = FragView(this, inflater);
        _player?.let {
            view.onPlayerAvailable(it);
        }
        _view = view;
        return view;
    }
    override fun onDestroyMainView() {
        super.onDestroyMainView();
        _view?.cleanup();
        _view = null;
    }


    class FragView(frag: PlaybackFragment, inflater: LayoutInflater): MainFragView<PlaybackFragment>(frag, inflater, R.layout.fragment_player) {

        private val _imageArt: ShapeableImageView;
        private val _textTitle: TextView;
        private val _textArtist: TextView;
        private val _textAlbum: TextView;

        private val _buttonPlay: ImageButton;
        private val _buttonLeft: ImageButton;
        private val _buttonRight: ImageButton;

        private val _buttonShuffle: ImageButton;
        private val _buttonRepeat: ImageButton;

        private val _buttonStars: RatingButton;
        private val _buttonQueue: ImageButton;
        private val _buttonPlaylistAdd: ImageButton;

        private val _queueOverlay: QueueOverlay;

        private val _viewControls: PlayerControlView;
        private val _timeBar: TimeBar;

        private var _isPlaying: Boolean = false;

        private var _trackCurrent: DBTrack? = null;

        private val _listener = object: PlayerManager.Listener {
            override fun onPlayingChanged(isPlaying: Boolean) {
                this@FragView.onPlayingChanged(isPlaying);
            }

            override fun onMediaMetadataChanged(player: Player, mediaMetadata: MediaMetadata?) {
                //this@FragView.onMediaMetadataChanged(player, mediaMetadata);
            }

            override fun onMediaItemChanged(player: Player, mediaItem: MediaItem?, reason: Int) {
                val id = mediaItem?.mediaId?.toLongOrNull();
                if(id != null) {
                    setCurrentTrack(id);
                }
                if(mediaItem != null && mediaItem.mediaMetadata.title != null) {
                    this@FragView.onMediaMetadataChanged(player, mediaItem.mediaMetadata, );
                }
            }

            override fun onMediaClose() {
            }
        }

        init {
            _imageArt = findViewById(R.id.image_art);

            _textTitle = findViewById(R.id.text_title);
            _textAlbum = findViewById(R.id.text_album);
            _textArtist = findViewById(R.id.text_artist);

            _textTitle.isSelected = true;


            _buttonLeft = findViewById(R.id.button_left);
            _buttonRight = findViewById(R.id.button_right);
            _buttonPlay = findViewById(R.id.button_play);

            _buttonShuffle = findViewById(R.id.button_shuffle);
            _buttonRepeat = findViewById(R.id.button_loop);

            _buttonStars = findViewById(R.id.button_stars);
            _buttonQueue = findViewById(R.id.button_queue);
            _buttonPlaylistAdd = findViewById(R.id.button_add_playlist);
            _queueOverlay = findViewById(R.id.overlay_queue);

            _buttonShuffle.setOnClickListener {
                val player = frag?._player?.player ?: return@setOnClickListener;
                player.shuffleModeEnabled = !player.shuffleModeEnabled;
                setShuffleButtonState(player.shuffleModeEnabled);
            }
            _buttonRepeat.setOnClickListener {
                val player = frag?._player?.player ?: return@setOnClickListener;
                if(player.repeatMode == Player.REPEAT_MODE_OFF)
                    player.repeatMode = Player.REPEAT_MODE_ALL;
                else if(player.repeatMode == Player.REPEAT_MODE_ALL)
                    player.repeatMode = Player.REPEAT_MODE_ONE;
                else
                    player.repeatMode = Player.REPEAT_MODE_OFF;

                setRepeatButtonState(player.repeatMode);
            }

            _viewControls = findViewById(R.id.view_control);
            _timeBar = _viewControls.findViewById(R.id.time_progress);

            _buttonPlay.setOnClickListener {
                if(_isPlaying)
                    fragment._player?.player?.pause();
                else
                    fragment._player?.player?.play();
            }
            _buttonLeft.setOnClickListener {
                if(fragment._player?.player?.hasPreviousMediaItem() ?: false) {
                    fragment._player?.player?.seekToPreviousMediaItem();
                }
            }
            _buttonRight.setOnClickListener {
                if(fragment._player?.player?.hasNextMediaItem() ?: false) {
                    fragment._player?.player?.seekToNextMediaItem();
                }
            }

            _buttonStars.onClick.subscribe {
                val trackNow = _trackCurrent;
                if(trackNow != null) {
                    val queued = StateQueue.instance.getQueuePlayable();

                    fragment.lifecycleScope.launch(Dispatchers.IO) {
                        val track = StateDatabase.instance.getTrack(trackNow.id);
                        val playlist = if(queued is DBPlaylist) StateDatabase.instance.getPlaylist(queued.id) else null;
                        val album = if(queued is DBAlbum) StateDatabase.instance.getAlbum(queued.id) else null;
                        val artist = if(queued is DBArtist) StateDatabase.instance.getArtist(queued.id) else null;

                        withContext(Dispatchers.Main) {
                            UIDialogs.showRatingDialog(context, fragment.lifecycleScope, track, playlist, album, artist);
                        }
                    }
                }
            }

            _buttonQueue.setOnClickListener {
                val currentPlayable = StateQueue.instance.getQueuePlayable() ?: return@setOnClickListener;
                val currentTracks = StateQueue.instance.getQueue();

                showQueue(currentPlayable, currentTracks);
            }
            _queueOverlay.onClose.subscribe {
                hideQueue();
            }
            _queueOverlay.onTrackClicked.subscribe {
                StateQueue.instance.setQueueCurrent(it);
                hideQueue();
            }
            _queueOverlay.onTrackRemoved.subscribe {
                StateQueue.instance.removeQueueItem(it);
                val currentPlayable = StateQueue.instance.getQueuePlayable() ?: return@subscribe;
                val currentTracks = StateQueue.instance.getQueue();
                _queueOverlay.setPlayable(currentPlayable, currentTracks);
            }
            _queueOverlay.onTracksOrderChanged.subscribe {
                StateQueue.instance.setQueueModify(context, it);
            }

            _buttonPlaylistAdd.setOnClickListener {
                val track = _trackCurrent;
                if(track != null) {
                    UIDialogs.showAddToPlaylistDialog(context, fragment.lifecycleScope, "Select which playlist you'd like to add this track to.", track);
                }
                else UIDialogs.appToast("No current track set?");
            }


            frag._player?.let {
                onPlayerAvailable(it);
            }
        }

        fun showQueue(playable: IPlayable, tracks: List<IPlayableTrack>) {
            _queueOverlay.setPlayable(playable, tracks);

            _queueOverlay.translationY = 1f;
            _queueOverlay.visibility = VISIBLE;

            val animations = arrayListOf<Animator>();
            animations.add(ObjectAnimator.ofFloat(_queueOverlay, "translationY", _queueOverlay.measuredHeight.toFloat(), 0.0f)
                .setDuration(200L).apply {
                    this.interpolator = AccelerateDecelerateInterpolator()
                })

            val animatorSet = AnimatorSet();
            animatorSet.playTogether(animations);
            animatorSet.start();
        }
        fun hideQueue() {

            _queueOverlay.translationY = 0f;

            val animations = arrayListOf<Animator>();
            animations.add(ObjectAnimator.ofFloat(_queueOverlay, "translationY", 0.0f, _queueOverlay.measuredHeight.toFloat())
                .setDuration(200L).apply {
                    this.interpolator = AccelerateDecelerateInterpolator()
                })

            val animatorSet = AnimatorSet();
            animatorSet.playTogether(animations);
            animatorSet.start();
        }

        fun setCurrentTrack(id: Long) {
            fragment.lifecycleScope.launch(Dispatchers.IO) {
                val track = StateDatabase.instance.getTrack(id);
                _trackCurrent = track;

                withContext(Dispatchers.Main) {
                    _buttonStars.setRating(track?.score ?: 0);
                    if(track != null)
                        setTrackMetadata(track);
                    _queueOverlay.setCurrentTrack(track);
                }
            }
        }

        fun onPlayingChanged(isPlaying: Boolean) {
            _isPlaying = isPlaying;
            fragment.lifecycleScope.launch(Dispatchers.Main) {
                setPlayButtonState(isPlaying);
            }
        }
        fun onMediaMetadataChanged(player: Player, mediaMetadata: MediaMetadata?, track: DBTrack? = null) {
            val abc = "";
            fragment.lifecycleScope.launch(Dispatchers.Main) {
                if(mediaMetadata == null)
                    return@launch; //TODO: Clear;
                try {

                    setPlaySkipButtonStates(player.hasPreviousMediaItem(), player.hasNextMediaItem());
                    _imageArt.setAlbumArt(mediaMetadata);
                    _textTitle.text = (track?.name ?: mediaMetadata.title);
                    _textArtist.text = (track?.artistLine ?: mediaMetadata.artist);
                    _textAlbum.text = (track?.albumLine ?: mediaMetadata.albumTitle);

                }
                catch(ex: Throwable) {

                }
            }
        }
        fun setTrackMetadata(track: DBTrack) {
            _textTitle.text = track.name;
            _textArtist.text = track.artistLine;
            _textAlbum.text = track.albumLine;
        }


        fun setRepeatButtonState(repeat: Int) {
            if(repeat == Player.REPEAT_MODE_OFF)
                _buttonRepeat.setImageResource(R.drawable.ic_repeat);
            else if(repeat == Player.REPEAT_MODE_ALL)
                _buttonRepeat.setImageResource(R.drawable.ic_repeat_active);
            else if(repeat == Player.REPEAT_MODE_ONE)
                _buttonRepeat.setImageResource(R.drawable.ic_repeat_one_active);
        }
        fun setShuffleButtonState(shuffling: Boolean) {
            _buttonShuffle.setImageResource(
                if(shuffling)
                    R.drawable.ic_shuffle_active
                else
                    R.drawable.ic_shuffle
            )
        }
        fun setPlayButtonState(isPlaying: Boolean) {
            if(isPlaying)
                _buttonPlay.setImageResource(R.drawable.ic_pause);
            else
                _buttonPlay.setImageResource(R.drawable.ic_play);
        }
        fun setPlaySkipButtonStates(prev: Boolean, next: Boolean) {
            if(prev)
                _buttonLeft.alpha = 1f;
            else
                _buttonLeft.alpha = 0.3f;
            if(next)
                _buttonRight.alpha = 1f;
            else
                _buttonRight.alpha = 0.3f;
        }


        fun onPlayerAvailable(player: PlayerManager) {
            _viewControls.player = player.player;
            player.subscribeAndInvoke(this, _listener);

            setRepeatButtonState(player.player.repeatMode);
            setShuffleButtonState(player.player.shuffleModeEnabled);

            _viewControls.setProgressUpdateListener({ position: Long, bufferedPosition: Long ->
                _timeBar.setDuration(player.player.duration);
                _timeBar.setPosition(position);
                _timeBar.setBufferedPosition(bufferedPosition);
            });
            _timeBar.addListener(object : TimeBar.OnScrubListener {
                override fun onScrubStart(timeBar: TimeBar, position: Long) {
                    player.player.seekTo(position);
                }

                override fun onScrubMove(timeBar: TimeBar, position: Long) {
                    player.player.seekTo(position);
                }

                override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                    player.player.seekTo(position);
                }
            })
        }

        fun onShown(parameter: Any? = null) {
            if(parameter is IPlayable) {
                val player = fragment?._player?.player;
                if(player != null) {
                    player.shuffleModeEnabled = false;
                    setShuffleButtonState(player.shuffleModeEnabled);
                }
                StateQueue.instance.setQueue(context, parameter);
            }
            if(parameter is IPlayableWithPlaySettings) {
                if(parameter.playSettings.shuffle) {
                    val player = fragment?._player?.player;
                    if(player != null) {
                        player.shuffleModeEnabled = true;
                        setShuffleButtonState(player.shuffleModeEnabled);
                    }
                }
                StateQueue.instance.setQueue(context, parameter.playable);
            }
        }

        fun onHide() {
            
        }

        fun cleanup(){
            fragment._player?.unsubscribe(this);
        }
    }
}