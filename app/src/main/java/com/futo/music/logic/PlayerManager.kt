package com.futo.music.logic

import android.R
import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
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
    var isEnded: Boolean = false;

    val onPlayingChanged = Event1<Boolean>();
    val onMediaMetadataChanged = Event1<MediaMetadata>();
    val onMediaItemChanged = Event2<MediaItem?, Int>();
    val onMediaClose = Event0();

    private val _listeners = mutableMapOf<Any, Listener>();

    private val _progressMonitor: Handler;

    private val _listenerPlayer = object: Player.Listener {
        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            super.onTimelineChanged(timeline, reason)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            this@PlayerManager.isPlaying = isPlaying;
            onPlayingChanged.emit(isPlaying);
            invokeListener {
                it.onPlayingChanged((isPlaying || player.playWhenReady) && !isEnded);
            }
        }
        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState);

            when(playbackState) {
                Player.STATE_READY -> {
                    isEnded = false;
                }
                Player.STATE_ENDED -> {
                    onMediaClose.emit();
                    invokeListener {
                        it.onMediaClose();
                    }
                    isEnded = true;
                }
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
    }

    constructor(player: Player) {
        this.player = player;
        player.addListener(_listenerPlayer);
        this.isPlaying = player.isPlaying;
        this.lastMediaMetadata = player.mediaMetadata;
        this.lastMediaItem = PlaybackService.getCurrentMediaItem() //Temporary workaround

        _progressMonitor = Handler(Looper.getMainLooper()!!);
        var progressChangedRunnable: Runnable? = null;
        progressChangedRunnable = object: Runnable {
            override fun run() {
                if(isPlaying) {
                    invokeListener {
                        it.onProgressChanged(player.currentPosition, player.contentDuration);
                    }
                }
                _progressMonitor.postDelayed(progressChangedRunnable!!, 500);
            }
        };
        _progressMonitor.postDelayed(progressChangedRunnable, 500);
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
        listener.onProgressChanged(player.currentPosition, player.contentDuration);
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



    interface Listener {
        fun onPlayingChanged(isPlaying: Boolean);
        fun onMediaMetadataChanged(player: Player, mediaMetadata: MediaMetadata?);
        fun onMediaItemChanged(player: Player, mediaItem: MediaItem?, reason: Int);
        fun onMediaClose();

        fun onProgressChanged(progressMs: Long, contentLengthMs: Long);

    }

    companion object {
        val TAG = "PlayerManager";
    }
}