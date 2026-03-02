package com.futo.music.logic

import android.R
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import com.futo.music.constructs.Event1
import com.futo.music.constructs.Event2

class PlayerManager {

    val player: Player

    var lastMediaMetadata: MediaMetadata? = null;
    var isPlaying: Boolean = false;

    val onPlayingChanged = Event1<Boolean>();
    val onMediaMetadataChanged = Event1<MediaMetadata>();
    val onMediaItemChanged = Event2<MediaItem?, Int>();

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
                it.onPlayingChanged(isPlaying);
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            onMediaItemChanged.emit(mediaItem, reason);
            invokeListener {
                it.onMediaItemChanged(player, mediaItem, reason);
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
    }
}