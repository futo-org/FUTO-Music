package com.futo.music.services

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.futo.music.RootApplication
import com.futo.music.activities.MainActivity
import com.futo.music.logging.Logger
import com.futo.music.logic.PlayerManager
import com.futo.music.models.playable.Track
import com.futo.music.states.StateApp
import com.futo.music.states.StateQueue
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlaybackService: MediaLibraryService() {

    private var _mediaSession: MediaLibrarySession? = null;

    private val serviceIOScope = CoroutineScope(Dispatchers.IO)
    private val serviceMainScope = CoroutineScope(Dispatchers.Main);



    private var _player: Player? = null;
    private var _audioManager: AudioManager? = null;
    var hasFocus: Boolean = false
        get() = field;
        set(value: Boolean) { field = value };
    var isTransientLoss: Boolean = false
        get() = field;
        set(value: Boolean) { field = value };

    private var _lastAudioFocusAttempt: Long = -1;
    private var _audioFocusLossTime: Long? = null;

    private var _focusRequest: AudioFocusRequest? = null;

    private val _callback = object: MediaLibrarySession.Callback {

        override fun onGetItem(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, mediaId: String): ListenableFuture<LibraryResult<MediaItem>> {
            return super.onGetItem(session, browser, mediaId)
        }

        /*
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>
        ): ListenableFuture<List<MediaItem>> {

            val mediaItemsUpdated = mediaItems.map {
                it.buildUpon()
                    .setMediaMetadata(it.mediaMetadata
                        .buildUpon()
                        .setTitle(it.mediaMetadata.title ?: "Temp")
                        .setExtras(Bundle().apply {
                            this.putString("mediaId", it.mediaId)
                        })
                        .build())
                    .setRequestMetadata(MediaItem.RequestMetadata.Builder()
                        .setExtras(Bundle().apply {
                            this.putString("mediaId", it.mediaId)
                        })
                        .build())
                    .build();
            }

            return Futures.immediateFuture(mediaItemsUpdated);F
        }
        */

        @OptIn(UnstableApi::class)
        override fun onAddMediaItems(mediaSession: MediaSession, controller: MediaSession.ControllerInfo, mediaItems: MutableList<MediaItem>): ListenableFuture<List<MediaItem>> {
            _lastMediaItems = mediaItems;

            return super.onAddMediaItems(mediaSession, controller, mediaItems)
        }

        @OptIn(UnstableApi::class)
        override fun onSetMediaItems(mediaSession: MediaSession, controller: MediaSession.ControllerInfo, mediaItems: MutableList<MediaItem>, startIndex: Int, startPositionMs: Long): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            _lastMediaItems = mediaItems;

            return super.onSetMediaItems(mediaSession, controller, mediaItems, startIndex, startPositionMs)
        }

        /*//TODO: Get queue
        override fun onPlaybackResumption(mediaSession: MediaSession, controller: MediaSession.ControllerInfo, isForPlayback: Boolean,): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val settable = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
            serviceIOScope.launch {
                settable.set(null)
            }
            return settable
        }
        */

        /*//TODO: Create media structure
        override fun onGetLibraryRoot(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, params: LibraryParams?): ListenableFuture<LibraryResult<MediaItem>> {
            return Futures.immediateFuture()
        }
        */
        /*//TODO: Create media structure
        override fun onGetChildren(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, parentId: String, page: Int, pageSize: Int, params: LibraryParams?): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            return Futures.immediateFuture()
        }
        */
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()


        val mediaFactory = object : MediaSource.Factory {
            override fun setDrmSessionManagerProvider(drmSessionManagerProvider: DrmSessionManagerProvider): MediaSource.Factory {
                throw NotImplementedError()
            }

            override fun setLoadErrorHandlingPolicy(loadErrorHandlingPolicy: LoadErrorHandlingPolicy): MediaSource.Factory {
                throw NotImplementedError()
            }

            override fun getSupportedTypes(): IntArray {
                throw NotImplementedError()
            }

            override fun createMediaSource(mediaItem : MediaItem) : MediaSource {
                    return ProgressiveMediaSource.Factory(
                        DefaultDataSource.Factory(this@PlaybackService)
                    ).createMediaSource(mediaItem)
            }
        }
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaFactory)
            .build();

        player.addListener(object: Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason);
                if(mediaItem?.localConfiguration?.uri != null) {
                    var items = getLastMediaItems();
                    if(items.size == 0)
                        items = StateQueue.instance.getLastMediaItems() ?: listOf();
                    _currentMediaItem = items.find { it.localConfiguration?.uri == mediaItem?.localConfiguration?.uri };
                }
                else
                    _currentMediaItem = null;
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)

                try {
                    if (isPlaying)
                        setAudioFocus();
                    else if (StateQueue.instance.isQueueEmpty)
                        abandonAudioFocus();
                }
                catch(ex: Throwable) {
                    Logger.e(PlayerManager.Companion.TAG, "Audio focus change failed", ex);
                }
            }
        });

        //Workaround for sanitization for now.
        _lastPlayer = player;
        _player = player;

        _mediaSession = MediaLibrarySession.Builder(this, player, _callback)
            .build();
        _mediaSession?.setSessionActivity(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT));

        this._audioManager = RootApplication.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager;
        StateQueue.instance.onQueueChanged.subscribe(this) {
            try {
                if(StateQueue.instance.isQueueEmpty && hasFocus)
                    abandonAudioFocus();
            }
            catch(ex: Throwable) {
                Logger.e(PlayerManager.Companion.TAG, "Failed to abandon focus");
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = _mediaSession;

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        _mediaSession?.run {
            player?.pause();
        }
    }

    override fun onDestroy() {
        _mediaSession?.run {
            player.release();
            release()
            _mediaSession = null;
        }
        super.onDestroy()
    }



    private fun setAudioFocus() {
        if (!(_player?.playWhenReady ?: false)) {
            return
        }

        if (hasFocus || isTransientLoss) {
            return;
        }

        val now = System.currentTimeMillis()
        val lastAudioFocusAttempt_ms = _lastAudioFocusAttempt
        if (lastAudioFocusAttempt_ms == null || now - lastAudioFocusAttempt_ms > 1000) {
            _lastAudioFocusAttempt = now
        } else {
            Log.v(PlayerManager.Companion.TAG, "Skipped trying to get audio focus because gaining audio focus was recently attempted.");
            return
        }

        if (_focusRequest == null) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(_audioFocusChangeListener)
                .build()

            _focusRequest = focusRequest;
            Log.i(PlayerManager.Companion.TAG, "Created audio focus request.");
        }

        Log.i(PlayerManager.Companion.TAG, "Requesting audio focus.");

        val result = _audioManager?.requestAudioFocus(_focusRequest!!)
        Log.i(PlayerManager.Companion.TAG, "Audio focus request result $result");
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            hasFocus = true
            isTransientLoss = false
            Log.i(PlayerManager.Companion.TAG, "Audio focus received");
        } else if (result == AudioManager.AUDIOFOCUS_REQUEST_DELAYED) {
            hasFocus = false
            isTransientLoss = false
            Log.i(PlayerManager.Companion.TAG, "Audio focus delayed, waiting for focus")
        } else {
            hasFocus = false
            isTransientLoss = false
            Log.i(PlayerManager.Companion.TAG, "Audio focus not granted, retrying later")
        }

        Log.i(PlayerManager.Companion.TAG, "Audio focus requested.");
    }
    private fun abandonAudioFocus() {
        val focusRequest = _focusRequest;
        if (focusRequest != null) {
            Logger.i(PlayerManager.Companion.TAG, "Audio focus abandoned")
            _audioManager?.abandonAudioFocusRequest(focusRequest);
            _focusRequest = null;
        }
        hasFocus = false;
        isTransientLoss = false;
    }

    private val _audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        try {
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    hasFocus = true;
                    isTransientLoss = false;

                    val audioFocusLossDuration =
                        _audioFocusLossTime?.let { System.currentTimeMillis() - it }
                    _audioFocusLossTime = null

                    Log.i(PlayerManager.Companion.TAG, "Audio focus gained");

                    if (audioFocusLossDuration == null) return@OnAudioFocusChangeListener
                    if(audioFocusLossDuration < 10_000)
                        _player?.play();
                }

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    val wasPlaying = _player?.playWhenReady ?: false;
                    _audioFocusLossTime = if (wasPlaying) System.currentTimeMillis() else null

                    hasFocus = false;
                    isTransientLoss = true;
                    _player?.pause();
                    Log.i(PlayerManager.Companion.TAG, "Audio focus transient loss (_audioFocusLossTime_ms = ${_audioFocusLossTime})");
                }

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    Log.i(PlayerManager.Companion.TAG, "Audio focus transient loss, can duck");
                    hasFocus = true;
                    isTransientLoss = true;
                }

                AudioManager.AUDIOFOCUS_LOSS -> {
                    val wasPlaying = _player?.playWhenReady ?: false;
                    _audioFocusLossTime = if (wasPlaying) System.currentTimeMillis() else null

                    _player?.pause();
                    abandonAudioFocus();
                    Log.i(PlayerManager.Companion.TAG, "Audio focus lost");
                }
            }
        } catch (ex: Throwable) {
            Logger.w(PlayerManager.Companion.TAG, "Failed to handle audio focus event", ex);
        }
    }

    companion object {
        val TAG = "PlaybackService";

        var _currentMediaItem: MediaItem? = null;
        var _lastMediaItems: List<MediaItem>? = null;
        var _lastPlayer: Player? = null;

        fun getLastPlayer(): Player? {
            return _lastPlayer;
        }

        fun getLastMediaItems(): List<MediaItem> {
            return _lastMediaItems ?: listOf();
        }
        fun getCurrentMediaItem(): MediaItem? = _currentMediaItem;
    }
}