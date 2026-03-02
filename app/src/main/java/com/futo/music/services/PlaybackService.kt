package com.futo.music.services

import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
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
import com.futo.music.logging.Logger
import com.futo.music.models.playable.Track
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
        @OptIn(UnstableApi::class)
        override fun onSetMediaItems(mediaSession: MediaSession, controller: MediaSession.ControllerInfo, mediaItems: MutableList<MediaItem>, startIndex: Int, startPositionMs: Long): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val newItems = mediaItems.map {
                it.buildUpon().setUri(it.mediaId).build()
            }.toMutableList()

            return super.onSetMediaItems(mediaSession, controller, newItems, startIndex, startPositionMs)
        }
        */

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

        _mediaSession = MediaLibrarySession.Builder(this, player, _callback)
            .build();
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
    }
}