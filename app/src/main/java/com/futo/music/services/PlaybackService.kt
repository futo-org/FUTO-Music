package com.futo.music.services

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import com.futo.music.activities.MainActivity
import com.futo.music.logging.Logger
import com.futo.music.models.playable.Track
import com.futo.music.states.StateApp
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

            return Futures.immediateFuture(mediaItemsUpdated);
        }
        */

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
                    return ProgressiveMediaSource.Factory(DefaultDataSource.Factory(this@PlaybackService))
                        .createMediaSource(MediaItem.fromUri(mediaItem.localConfiguration?.uri ?: Uri.EMPTY));
            }
        }
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaFactory)
            .build();

        player.addListener(object: Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason);
                if(mediaItem?.localConfiguration?.uri != null)
                    _currentMediaItem = getLastMediaItems().find { it.localConfiguration?.uri == mediaItem?.localConfiguration?.uri };
                else
                    _currentMediaItem = null;
            }
        });

        //Workaround for sanitization for now.
        _lastPlayer = player;

        _mediaSession = MediaLibrarySession.Builder(this, player, _callback)
            .build();
        _mediaSession?.setSessionActivity(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT));
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = _mediaSession;

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        _mediaSession?.run {
            player.release();
            release()
            _mediaSession = null;
        }
        super.onDestroy()
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