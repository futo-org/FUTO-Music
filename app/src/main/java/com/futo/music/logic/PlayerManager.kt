package com.futo.music.logic

import android.R
import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import com.futo.music.RootApplication
import com.futo.music.constructs.Event0
import com.futo.music.constructs.Event1
import com.futo.music.constructs.Event2
import com.futo.music.logging.Logger
import com.futo.music.services.PlaybackService
import com.futo.music.states.StateQueue

class PlayerManager {

    val player: Player


    var lastMediaMetadata: MediaMetadata? = null;
    var lastMediaItem: MediaItem? = null;
    var isPlaying: Boolean = false;


    var hasFocus: Boolean = false
        get() = field;
        set(value: Boolean) { field = value };
    var isTransientLoss: Boolean = false
        get() = field;
        set(value: Boolean) { field = value };

    private var _lastAudioFocusAttempt: Long = -1;
    private var _audioFocusLossTime: Long? = null;

    private var _focusRequest: AudioFocusRequest? = null;
    private var _audioManager: AudioManager? = null;

    val onPlayingChanged = Event1<Boolean>();
    val onMediaMetadataChanged = Event1<MediaMetadata>();
    val onMediaItemChanged = Event2<MediaItem?, Int>();
    val onMediaClose = Event0();

    private val _listeners = mutableMapOf<Any, Listener>();

    private val _listenerPlayer = object: Player.Listener {
        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            super.onTimelineChanged(timeline, reason)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            this@PlayerManager.isPlaying = isPlaying;
            onPlayingChanged.emit(isPlaying);
            invokeListener {
                it.onPlayingChanged(isPlaying || player.playWhenReady);
            }

            try {
                if (isPlaying)
                    setAudioFocus();
                else if (StateQueue.instance.isQueueEmpty)
                    abandonAudioFocus();
            }
            catch(ex: Throwable) {
                Logger.e(TAG, "Audio focus change failed", ex);
            }
        }

        override fun onMediaItemTransition(mediaItemOriginal: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItemOriginal, reason)

            val currentMediaItem = player.currentMediaItem;
            //TODO: Remove this hackfix once metadata restore is fixed
            val mediaItem = StateQueue.instance.restoreMediaItem(mediaItemOriginal) ?: mediaItemOriginal;
            lastMediaItem = mediaItem;
            onMediaItemChanged.emit(mediaItem, reason);
            if(mediaItem?.mediaMetadata != null && mediaItem?.mediaMetadata?.title?.isNotEmpty() == true)
                onMediaMetadataChanged.emit(mediaItem.mediaMetadata);
            invokeListener {
                it.onMediaItemChanged(player, mediaItem, reason);
                if(mediaItem?.mediaMetadata != null && mediaItem?.mediaMetadata?.title?.isNotEmpty() == true)
                    it.onMediaMetadataChanged(player, mediaItem.mediaMetadata)
            }
        }
        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            super.onMediaMetadataChanged(mediaMetadata)
            lastMediaMetadata = mediaMetadata;
            onMediaMetadataChanged.emit(mediaMetadata);
            invokeListener {
                it.onMediaMetadataChanged(player, mediaMetadata);
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState);

            when(playbackState) {
                Player.STATE_ENDED -> {
                    onMediaClose.emit();
                    invokeListener {
                        it.onMediaClose();
                    }
                }
            }
        }
    }

    constructor(player: Player) {
        this.player = player;
        player.addListener(_listenerPlayer);
        this.isPlaying = player.isPlaying;
        this.lastMediaMetadata = player.mediaMetadata;
        this.lastMediaItem = PlaybackService.getCurrentMediaItem() //Temporary workaround
        this._audioManager = RootApplication.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager;

        StateQueue.instance.onQueueChanged.subscribe(this) {
            try {
                if (StateQueue.instance.isQueueEmpty && hasFocus)
                    abandonAudioFocus();
            }
            catch(ex: Throwable) {
                Logger.e(TAG, "Failed to abandon focus");
            }
        }
    }

    fun subscribe(tag: Any, listener: Listener) {
        synchronized(_listeners) {
            _listeners.put(tag, listener);
        }
    }
    fun subscribeAndInvoke(tag: Any, listener: Listener) {
        synchronized(_listeners) {
            _listeners.put(tag, listener);
        }
        listener.onPlayingChanged(isPlaying);
        listener.onMediaMetadataChanged(player, lastMediaMetadata);
        listener.onMediaItemChanged(player, lastMediaItem, -1);
    }
    fun unsubscribe(tag: Any) {
        synchronized(_listeners) {
            _listeners.remove(tag);
        }
    }

    private fun invokeListener(handle: (listener: Listener)->Unit) {
        val toInvoke = synchronized(_listeners) { _listeners.values.toList() };
        for(listener in toInvoke)
            handle(listener);
    }

    private fun setAudioFocus() {
        if (!isPlaying) {
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
            Log.v(TAG, "Skipped trying to get audio focus because gaining audio focus was recently attempted.");
            return
        }

        if (_focusRequest == null) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(_audioFocusChangeListener)
                .build()

            _focusRequest = focusRequest;
            Log.i(TAG, "Created audio focus request.");
        }

        Log.i(TAG, "Requesting audio focus.");

        val result = _audioManager?.requestAudioFocus(_focusRequest!!)
        Log.i(TAG, "Audio focus request result $result");
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            hasFocus = true
            isTransientLoss = false
            Log.i(TAG, "Audio focus received");
        } else if (result == AudioManager.AUDIOFOCUS_REQUEST_DELAYED) {
            hasFocus = false
            isTransientLoss = false
            Log.i(TAG, "Audio focus delayed, waiting for focus")
        } else {
            hasFocus = false
            isTransientLoss = false
            Log.i(TAG, "Audio focus not granted, retrying later")
        }

        Log.i(TAG, "Audio focus requested.");
    }

    private fun abandonAudioFocus() {
        val focusRequest = _focusRequest;
        if (focusRequest != null) {
            Logger.i(TAG, "Audio focus abandoned")
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

                    Log.i(TAG, "Audio focus gained");

                    if (audioFocusLossDuration == null) return@OnAudioFocusChangeListener
                    if(audioFocusLossDuration < 10_000)
                        player.play();
                }

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    val wasPlaying = isPlaying
                    _audioFocusLossTime = if (wasPlaying) System.currentTimeMillis() else null

                    hasFocus = false;
                    isTransientLoss = true;
                    player.pause();
                    Log.i(TAG, "Audio focus transient loss (_audioFocusLossTime_ms = ${_audioFocusLossTime})");
                }

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    Log.i(TAG, "Audio focus transient loss, can duck");
                    hasFocus = true;
                    isTransientLoss = true;
                }

                AudioManager.AUDIOFOCUS_LOSS -> {
                    val wasPlaying = isPlaying
                    _audioFocusLossTime = if (wasPlaying) System.currentTimeMillis() else null

                    player.pause();
                    abandonAudioFocus();
                    Log.i(TAG, "Audio focus lost");
                }
            }
        } catch (ex: Throwable) {
            Logger.w(TAG, "Failed to handle audio focus event", ex);
        }
    }

    interface Listener {
        fun onPlayingChanged(isPlaying: Boolean);
        fun onMediaMetadataChanged(player: Player, mediaMetadata: MediaMetadata?);
        fun onMediaItemChanged(player: Player, mediaItem: MediaItem?, reason: Int);
        fun onMediaClose();
    }

    companion object {
        val TAG = "PlayerManager";
    }
}