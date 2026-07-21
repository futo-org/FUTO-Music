package com.futo.music.fragments.main

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.TimeBar
import com.futo.music.IPlayableWithPlaySettings
import com.futo.music.R
import com.futo.music.UIDialogs
import com.futo.music.dp
import com.futo.music.extensions.setAlbumArt
import com.futo.music.extractBitmap
import com.futo.music.fragments.MainFragView
import com.futo.music.gestures.OnSwipeTouchListener
import com.futo.music.logic.PlayerManager
import com.futo.music.models.playable.IPlayable
import com.futo.music.models.playable.IPlayableTrack
import com.futo.music.models.playable.QueueType
import com.futo.music.models.playable.Track
import com.futo.music.services.PlaybackService
import com.futo.music.states.DBPlayableType
import com.futo.music.states.StateApp
import com.futo.music.states.StateDatabase
import com.futo.music.states.StateQueue
import com.futo.music.storage.db.DBAlbum
import com.futo.music.storage.db.DBArtist
import com.futo.music.storage.db.DBPlaylist
import com.futo.music.storage.db.DBTrack
import com.futo.music.ui.buttons.RatingsButton
import com.futo.music.ui.views.StarButton
import com.futo.music.ui.views.playback.PlayingInfoOverlay
import com.futo.music.ui.views.playback.QueueOverlay
import com.google.android.material.imageview.ShapeableImageView
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaybackFragment: MainFragment() {
    override val isMainView : Boolean = true;
    override val isTab: Boolean = false;
    override val isHistory: Boolean = true;

    private var _player: PlayerManager? = null;

    private var _view: FragView? = null;

    fun setPlayer(player: PlayerManager) {
        _player = player;
        _view?.onPlayerAvailable(player);
    }

    override fun onBackPressed(): Boolean {
        return _view?.onBackPressed() ?: false;
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig);

        StateApp.instance.activity()?.let { act -> //Forces rerendering with different layout without recreating the entire MainActivity.
            if(act.fragCurrent == this) {
                parentFragmentManager.beginTransaction()
                    .detach(this).commitNow();
                parentFragmentManager.beginTransaction()
                    .attach(this).commitNow();
            }
        }
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

        private val _root: ConstraintLayout?;

        private val _imageArt: ShapeableImageView;
        private val _textTitle: TextView;
        private val _textArtist: TextView;
        private val _textAlbum: TextView;

        private val _imageArtist: ImageView;
        private val _imageAlbum: ImageView;

        private val _buttonBack: ImageButton;
        private val _buttonOptions: ImageButton;

        private val _buttonPlay: ImageButton;
        private val _buttonLeft: ImageButton;
        private val _buttonRight: ImageButton;

        private val _buttonShuffle: ImageButton;
        private val _buttonRepeat: ImageButton;

        private val _buttonInfo: ImageButton;
        private val _textRatingNote: TextView;
        private val _containerQueue: LinearLayout;
        private val _textQueue: TextView;
        private val _buttonPlaylistAdd: ImageButton;

        private val _starAlbum: StarButton?;
        private val _starArtist: StarButton?;

        private val _buttonsRating: RatingsButton;

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
                setPlayButtonState(false);
            }

            override fun onProgressChanged(progressMs: Long, contentLengthMs: Long) {

            }
        }

        init {
            _root = findViewById(R.id.root);

            _imageArt = findViewById(R.id.image_art);

            _textTitle = findViewById(R.id.text_title);
            _textAlbum = findViewById(R.id.text_album);
            _textArtist = findViewById(R.id.text_artist);

            _imageArtist = findViewById(R.id.image_artist);
            _imageAlbum = findViewById(R.id.image_album);


            _buttonsRating = findViewById(R.id.buttons_rating);
            _textRatingNote = findViewById(R.id.text_rating_note);

            _textTitle.isSelected = true;

            _buttonBack = findViewById(R.id.button_back);
            _buttonOptions = findViewById(R.id.button_options);

            _buttonLeft = findViewById(R.id.button_left);
            _buttonRight = findViewById(R.id.button_right);
            _buttonPlay = findViewById(R.id.button_play);

            _buttonShuffle = findViewById(R.id.button_shuffle);
            _buttonRepeat = findViewById(R.id.button_loop);

            _starArtist = findViewById(R.id.star_artist);
            _starAlbum = findViewById(R.id.star_album);

            _buttonInfo = findViewById(R.id.button_meta);
            _containerQueue = findViewById(R.id.container_queue);
            _textQueue = findViewById(R.id.text_queue);
            _buttonPlaylistAdd = findViewById(R.id.button_add_playlist);
            _queueOverlay = findViewById(R.id.overlay_queue);

            _buttonBack.setOnClickListener {
                fragment.closeSegment();
            }

            _buttonOptions.isVisible = false;

            _buttonShuffle.setOnClickListener {

                if(StateQueue.instance.queueType == QueueType.SmartShuffle) {
                    UIDialogs.appToast("You are playing a smart shuffle, you cannot change this.")
                    return@setOnClickListener;
                }
                if(StateQueue.instance.queueType == QueueType.Shuffle) {
                    UIDialogs.appToast("You are playing a shuffle, you cannot change this.")
                    return@setOnClickListener;
                }

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
            _viewControls.post {
                val timeBarRect = Rect();
                _timeBar.getHitRect(timeBarRect);

                _viewControls.setOnTouchListener { view, event ->
                    event.setLocation(event.x,
                        (timeBarRect.top + timeBarRect.height() - 1).toFloat()
                    );
                    _timeBar.onTouchEvent(event);
                }
            }

            _buttonPlay.setOnClickListener {
                if(fragment._player?.isEnded ?: false) {
                    fragment._player?.player?.seekTo(0);
                    fragment._player?.player?.play();
                }
                else {
                    if (_isPlaying)
                        fragment._player?.player?.pause();
                    else
                        fragment._player?.player?.play();
                }
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

            _buttonInfo.setOnClickListener {
                val trackNow = _trackCurrent;
                if(trackNow != null) {
                    /*
                    val queued = StateQueue.instance.getQueuePlayable();

                    fragment.lifecycleScope.launch(Dispatchers.IO) {
                        val track = StateDatabase.instance.getTrack(trackNow.id);
                        val playlist = if(queued is DBPlaylist) StateDatabase.instance.getPlaylist(queued.id) else null;
                        val album = if(queued is DBAlbum) StateDatabase.instance.getAlbum(queued.id) else null;
                        val artist = if(queued is DBArtist) StateDatabase.instance.getArtist(queued.id) else null;

                        withContext(Dispatchers.Main) {
                            UIDialogs.showRatingDialog(context, fragment.lifecycleScope, track, playlist, album, artist) {
                                fragment.lifecycleScope.launch(Dispatchers.IO) {
                                    val refetchTrack = StateDatabase.instance.getTrack(_trackCurrent?.id ?: return@launch) ?: return@launch;
                                    withContext(Dispatchers.Main) {
                                        //_buttonInfo.setRating(refetchTrack.score)
                                    }
                                }
                            };
                        }
                    }*/
                    showPlaying(trackNow);
                }
            }

            _containerQueue.setOnClickListener {
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
            _imageArt.setOnTouchListener(object: OnSwipeTouchListener(context) {
                override fun onSwipeBottom() {
                    super.onSwipeBottom();
                    fragment.closeSegment();
                }

                override fun onSwipeRight() {
                    super.onSwipeRight();
                    if(fragment._player?.player?.hasPreviousMediaItem() ?: false) {
                        fragment._player?.player?.seekToPreviousMediaItem();
                    }
                }

                override fun onSwipeLeft() {
                    super.onSwipeLeft();
                    if(fragment._player?.player?.hasNextMediaItem() ?: false) {
                        fragment._player?.player?.seekToNextMediaItem();
                    }
                }
            });


            frag._player?.let {
                onPlayerAvailable(it);
            }

            _textArtist.setOnClickListener {
                fragment.lifecycleScope.launch(Dispatchers.IO) {
                    val track = _trackCurrent ?: return@launch;
                    val artist = StateDatabase.instance.getTrackArtists(track.id).firstOrNull();
                    withContext(Dispatchers.Main) {
                        if(artist == null)
                            UIDialogs.appToast("Couldn't find artist?");
                        fragment.navigate<ArtistFragment>(artist);
                    }
                }
            }
            _textAlbum.setOnClickListener {
                fragment.lifecycleScope.launch(Dispatchers.IO) {
                    val track = _trackCurrent ?: return@launch;
                    val album = StateDatabase.instance.getTrackAlbums(track.id).firstOrNull();
                    withContext(Dispatchers.Main) {
                        if(album == null)
                            UIDialogs.appToast("Couldn't find album?");
                        fragment.navigate<AlbumFragment>(album);
                    }
                }
            }
        }


        fun onBackPressed(): Boolean {
            if(_isQueueVisible)
            {
                hideQueue();
                return true;
            }
            if(_isPlayingVisible) {
                hidePlaying();
                return true;
            }
            return false;
        }
        private var _isQueueVisible = false;
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
            _isQueueVisible = true;
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

            _isQueueVisible = false;
        }
        private var _isPlayingVisible = false;
        private var _lastPlayingDialog: Dialog? = null;
        fun showPlaying(playable: IPlayable) {
            if(playable !is DBTrack)
                return;

            UIDialogs.appToast("This UI is WIP");

            val view = PlayingInfoOverlay(context);
            view.onClose.subscribe {
                hidePlaying();
            }
            view.setPlayable(playable);

            val dialog = Dialog(context);
            //dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(view);
            dialog.window?.let {
                it.setWindowAnimations(R.anim.fade_in);
                it.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                it.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                it.setBackgroundDrawableResource(android.R.color.transparent);
                //WindowCompat.setDecorFitsSystemWindows(it, true);
                it.statusBarColor = Color.TRANSPARENT
                it.navigationBarColor = Color.TRANSPARENT
            }
            dialog.setOnDismissListener {
                _lastPlayingDialog = null;
                _isPlayingVisible = false;
                _root?.alpha = 1f;
            }
            _lastPlayingDialog = dialog;
            _isPlayingVisible = true;

            _root?.alpha = 0f;
            dialog.show();
        }
        fun hidePlaying() {
            _lastPlayingDialog?.hide();
            _lastPlayingDialog = null;
            _isPlayingVisible = false;
            _root?.alpha = 1f;
        }


        fun setCurrentTrack(id: Long) {
            fragment.lifecycleScope.launch(Dispatchers.IO) {
                val track = StateDatabase.instance.getTrack(id);
                _trackCurrent = track;

                withContext(Dispatchers.Main) {
                    //_buttonInfo.setRating(track?.score ?: 0);
                    _buttonsRating.setRatingsFor(track) { newRating, ghost, type ->
                        //_buttonInfo.setRating(newRating);
                        setRatingFrom(type);
                    }

                    if(track != null)
                        setTrackMetadata(track);
                    _queueOverlay.setCurrentTrack(track);
                }

                val artist = track?.artistId?.let {
                    StateDatabase.instance.getArtist(track.artistId);
                }
                val album = track?.mediaStoreAlbumId?.let {
                    StateDatabase.instance.getAlbumByMSID(track.mediaStoreAlbumId);
                }
                withContext(Dispatchers.Main) {
                    if(artist?.score != null) {
                        _starArtist?.setRating(artist.score);
                        _starArtist?.isVisible = true;
                    }
                    else
                        _starArtist?.isVisible = false;
                    if(album?.score != null) {
                        _starAlbum?.setRating(album.score);
                        _starAlbum?.isVisible = true;
                    }
                    else
                        _starAlbum?.isVisible = false;
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
                    updateQueueState();
                    /*_imageArt.setAlbumArt(mediaMetadata, colorIntercept = {
                        if((it?.dominant ?: it?.darkVibrant) != null)
                        StateApp.instance.activity()?.setBackgroundBottomGradient((it!!.dominant ?: it.darkVibrant!!), 0.5f, 0.6f);
                    })*/

                    val result = withContext(Dispatchers.IO) {
                        val trackImage = _trackCurrent?.getImage();
                        return@withContext trackImage;
                    }
                    if(result != null)
                        result.getGlideLoad(_imageArt)!!
                            .extractBitmap({
                                if(it != null)
                                    StateApp.instance.activity()?.let { act ->
                                        act.setBackgroundTop(BitmapDrawable(it), 2.3f, 0.4f, {
                                            it.transform(BlurTransformation(25, 3))
                                        });
                                        //act.setBackgroundBottomGradient(android.graphics.Color.BLACK, 0.3f);
                                    };
                            }).into(_imageArt);
                    else
                        _imageArt.setAlbumArt(mediaMetadata, bitmapIntercept = {
                            if(it != null)
                                StateApp.instance.activity()?.let { act ->
                                    act.setBackgroundTop(BitmapDrawable(it), 2.3f, 0.4f, {
                                        it.transform(BlurTransformation(25, 3))
                                    });
                                    //act.setBackgroundBottomGradient(android.graphics.Color.BLACK, 0.3f);
                                };
                        })
                    if(track == null)
                        return@launch;
                    _textTitle.text = (track?.name ?: mediaMetadata.title);
                    _textArtist.text = (track?.artistLine ?: mediaMetadata.artist);
                    _textAlbum.text = (track?.albumLine ?: mediaMetadata.albumTitle);

                    _imageArtist.isVisible = !_textArtist.text.isEmpty();
                    _imageAlbum.isVisible = !_textAlbum.text.isEmpty();

                }
                catch(ex: Throwable) {

                }
            }
        }
        fun setTrackMetadata(track: DBTrack) {
            _textTitle.text = track.name;
            _textArtist.text = track.artistLine;
            _textAlbum.text = track.albumLine;

            _imageArtist.isVisible = !_textArtist.text.isEmpty();
            _imageAlbum.isVisible = !_textAlbum.text.isEmpty();

            setRatingFrom(DBPlayableType.ofValue(track.scoreLevel));
        }

        fun setRatingFrom(type: DBPlayableType?) {
            if(type != DBPlayableType.Track && type != null) {
                _textRatingNote.text = "from " + type.name;
            }
            else
                _textRatingNote.text = "";
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
            when(StateQueue.instance.queueType) {
                QueueType.Shuffle -> _buttonShuffle.setImageResource(R.drawable.ic_shuffle_active);
                QueueType.SmartShuffle -> _buttonShuffle.setImageResource(R.drawable.ic_imagine_active);
                else -> {
                    _buttonShuffle.setImageResource(
                        if(shuffling)
                            R.drawable.ic_shuffle_active
                        else
                            R.drawable.ic_shuffle
                    )
                }
            }
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

        fun updateQueueState() {
            val queue = StateQueue.instance.getQueuePlayable();
            val queueList = StateQueue.instance.getQueue();
            if(queueList.size > 1) {
                val name = if(queue != null) queue.name else "Queue";
                _textQueue.text = name;
                _containerQueue.alpha = 1f;
            }
            else
                _containerQueue.alpha = 0f;
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
                StateQueue.instance.setQueue(context, parameter, {
                    if(it.size == 0) {
                        fragment.lifecycleScope.launch(Dispatchers.Main) {
                            fragment.closeSegment();
                            UIDialogs.appToast("No tracks in this collection");

                        }
                    }
                    fragment.lifecycleScope.launch(Dispatchers.Main) {
                        setShuffleButtonState(player?.shuffleModeEnabled ?: return@launch);
                    }
                });
                if(parameter is Track) {
                    _buttonsRating.setRatingsFor(null);
                    setRatingFrom(null);
                }
            }
            if(parameter is IPlayableWithPlaySettings) {
                if(parameter.playSettings.shuffle) {
                    val player = fragment?._player?.player;
                    if(player != null) {
                        player.shuffleModeEnabled = true;
                        setShuffleButtonState(player.shuffleModeEnabled);
                    }
                }
                StateQueue.instance.setQueue(context, parameter.playable, {
                    if(it.size == 0) {
                        fragment.lifecycleScope.launch(Dispatchers.Main) {
                            fragment.closeSegment();
                            UIDialogs.appToast("No tracks in this collection");

                        }
                    }
                    fragment.lifecycleScope.launch(Dispatchers.Main) {
                        val player = fragment._player?.player;
                        setShuffleButtonState(player?.shuffleModeEnabled ?: return@launch);
                    }
                }, parameter.playSettings.index);
            }
            if(parameter == null) {
                val player = PlaybackService._lastPlayer ?: return;
                val mediaItem = PlaybackService._currentMediaItem ?: return;

                val trackId = mediaItem?.mediaId?.toLongOrNull()
                if(trackId != null)
                    setCurrentTrack(trackId);
                onMediaMetadataChanged(player, mediaItem.mediaMetadata,);
            }
        }

        fun onHide() {
            
        }

        fun cleanup(){
            fragment._player?.unsubscribe(this);
        }
    }
}