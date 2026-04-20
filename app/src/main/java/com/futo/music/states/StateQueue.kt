package com.futo.music.states

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Artists
import android.webkit.MimeTypeMap
import androidx.core.database.getStringOrNull
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import com.futo.music.constructs.Event1
import com.futo.music.extensions.assume
import com.futo.music.logging.Logger
import com.futo.music.logic.PlayerManager
import com.futo.music.models.ImageVariable
import com.futo.music.models.playable.Album
import com.futo.music.models.playable.Artist
import com.futo.music.models.playable.IPlayable
import com.futo.music.models.playable.IPlayableTrack
import com.futo.music.models.playable.PlayableDescriptor
import com.futo.music.models.playable.PlayableType
import com.futo.music.models.playable.Track
import com.futo.music.services.PlaybackService
import com.futo.music.storage.db.DBAlbum
import com.futo.music.storage.db.DBArtist
import com.futo.music.storage.db.DBPlaylist
import com.futo.music.storage.db.DBTrack
import com.futo.music.storage.file.FragmentedStorage
import com.futo.music.storage.file.ManagedStore
import com.futo.music.storage.file.StringArrayStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.map
import kotlin.io.use
import kotlin.let
import kotlin.text.contains
import kotlin.text.isNullOrBlank
import kotlin.text.lowercase
import kotlin.text.startsWith
import kotlin.text.toLongOrNull
import kotlin.text.trim


class StateQueue {

    val _queue: MutableList<IPlayableTrack> = mutableListOf();
    val isQueueEmpty: Boolean get() = _queue.isEmpty();

    val onQueueChanged = Event1<List<IPlayableTrack>>();

    private var _player: PlayerManager? = null;

    private var _lastSetQueuePlayable: IPlayable? = null;
    private var _lastSetMediaItems: List<MediaItem>? = null;

    private val _lastSetQueuePlayableDescriptor: ManagedStore<PlayableDescriptor?> = FragmentedStorage.storeJson<PlayableDescriptor?>("lastPlayable").load();

    fun setPlayer(manager: PlayerManager) {
        _player = manager;
    }

    fun restoreQueue(context: Context) {
        val item = _lastSetQueuePlayableDescriptor.getItems().firstOrNull();
        if(item != null)
        {
            val restored = item.restore(context);
            if(restored != null) {
                _lastSetQueuePlayable = restored.item;
                if(restored.tracks.size > 0)
                    _lastSetMediaItems = restored.tracks.map { it.getMediaItem() };
            }
        }
    }
    fun setPersistentQueue(playable: IPlayable, tracks: List<IPlayableTrack>) {
        _lastSetQueuePlayableDescriptor.saveAllAsync(listOf(PlayableDescriptor(
            playable.type,
            tracks.mapNotNull { it.getItemId()?.toLongOrNull() },
            if(playable is DBAlbum)
                playable.id
            else if(playable is DBArtist)
                playable.id
            else if(playable is DBTrack)
                playable.id
            else if(playable is DBPlaylist)
                playable.id
            else
                -1
        )));
    }
    fun clearPersistentQueue() {
        _lastSetQueuePlayableDescriptor.saveAllAsync(listOf());
    }


    fun setLastMediaItems(items: List<MediaItem>) {
        _lastSetMediaItems = items;
    }
    fun getLastMediaItems(): List<MediaItem>? {
        return _lastSetMediaItems;
    }
    //Hackfix for missing data temporarily
    fun restoreMediaItem(item: MediaItem?): MediaItem? {
        if(item == null)
            return null;
        if(item.localConfiguration?.uri != null) {
            val original =
                PlaybackService.getLastMediaItems().find { it.localConfiguration?.uri != null && it.localConfiguration?.uri == item.localConfiguration?.uri } ?:
                _lastSetMediaItems?.find { it.localConfiguration?.uri != null && it.localConfiguration?.uri == item.localConfiguration?.uri };
            if(original != null)
                return original;
        }
        return item;
    }

    fun getQueuePlayable(): IPlayable? = _lastSetQueuePlayable;
    fun getQueue(): List<IPlayableTrack> {
        synchronized(_queue) {
            return _queue.toList();
        }
    }
    private fun setQueueState(items: List<IPlayableTrack>, playable: IPlayable?) {
        synchronized(_queue) {
            _queue.clear();
            _queue.addAll(items);
            _lastSetQueuePlayable = playable;
        }
    }
    fun setQueue(context: Context, playable: IPlayable) {
        StateApp.instance.scopeOrNull?.launch(Dispatchers.IO) {
            if(playable is DBAlbum)
                StateDatabase.instance.setPlayedAlbum(playable.id);
            else if(playable is DBArtist)
                StateDatabase.instance.setPlayedArtist(playable.id);
            else if(playable is DBTrack)
                StateDatabase.instance.setPlayedTrack(playable.id);
            else if(playable is DBPlaylist)
                StateDatabase.instance.setPlayedPlaylist(playable.id);


            val items = playable.getTracks(context);

            setPersistentQueue(playable, items)

            val newQueue: List<IPlayableTrack>;
            synchronized(_queue) {
                _queue.clear();
                _queue.addAll(items);
                _lastSetQueuePlayable = playable;
                newQueue = _queue.toList();
            }

            val mediaItems = newQueue.map { it.getMediaItem() }
            setLastMediaItems(mediaItems);
            withContext(Dispatchers.Main) {
                _player?.player?.setMediaItems(mediaItems);
                _player?.player?.prepare();
                _player?.player?.play();
            }
            onQueueChanged.emit(newQueue);
        }
    }

    fun clearQueue() {
        synchronized(_queue) {
            _queue.clear();
            _lastSetQueuePlayable = null;
            setLastMediaItems(listOf());
            clearPersistentQueue();
        }
        onQueueChanged.emit(listOf());
    }

    fun removeQueueItem(item: IPlayableTrack) {
        val player = _player?: return;

        val currentQueue = getQueue().toMutableList();
        val index = currentQueue.indexOf(item);
        if(index < 0)
            return;
        currentQueue.removeAt(index);
        setQueueState(currentQueue, _lastSetQueuePlayable);
        player.player.removeMediaItem(index);
    }

    fun setQueueCurrent(track: IPlayableTrack){
        val player = _player?: return;

        val currentQueue = getQueue();
        val index = currentQueue.indexOf(track);
        if(index < 0)
            return;

        player.player.seekTo(index, 0);
    }

    fun getCurrentTrack(): IPlayableTrack? {
        val currentIndex = _player?.player?.currentMediaItemIndex ?: return null;
        val currentQueue = getQueue();
        if(currentIndex < 0 || currentQueue.size <= currentIndex)
            return null;
        return currentQueue[currentIndex];
    }

    fun setQueueModify(context: Context, playable: IPlayable) {
        StateApp.instance.scopeOrNull?.launch(Dispatchers.IO) {
            if(playable is DBAlbum)
                StateDatabase.instance.setPlayedAlbum(playable.id);
            else if(playable is DBArtist)
                StateDatabase.instance.setPlayedArtist(playable.id);
            else if(playable is DBTrack)
                StateDatabase.instance.setPlayedTrack(playable.id);
            else if(playable is DBPlaylist)
                StateDatabase.instance.setPlayedPlaylist(playable.id);


            val items = playable.getTracks(context);
            setQueueModify(context, items);
        }
    }
    fun setQueueModify(context: Context, items: List<IPlayableTrack>) {

        val currentPlayable = getQueuePlayable() ?: return;
        val oldCurrentIndex = _player?.player?.currentMediaItemIndex ?: return setQueue(context, currentPlayable);
        val currentPlaying = getCurrentTrack() ?: return setQueue(context, currentPlayable);

        setPersistentQueue(currentPlayable, items)

        val newQueue: List<IPlayableTrack>;
        synchronized(_queue) {
            _queue.clear();
            _queue.addAll(items);
            _lastSetQueuePlayable = currentPlayable;
            newQueue = _queue.toList();
        }
        val currentPlayingNew = newQueue.find { it.getItemId() != null && it.getItemId() == currentPlaying.getItemId() } ?: setQueue(context, currentPlayable);
        val newCurrentIndex = newQueue.indexOf(currentPlayingNew);

        StateApp.instance.scopeOrNull?.launch(Dispatchers.IO) {
            val mediaItems = newQueue.map { it.getMediaItem() }
            withContext(Dispatchers.Main) {
                setLastMediaItems(mediaItems);
                StateApp?.instance?.scopeOrNull?.launch(Dispatchers.Main) {
                    val player = _player ?: return@launch;

                    //Reconstruct new queue without touching current item
                    for (i in 0..<oldCurrentIndex)
                        player.player.removeMediaItem(i);
                    for (i in 1..<player.player.mediaItemCount - 1)
                        player.player.removeMediaItem(i);
                    for (i in 0..<newCurrentIndex)
                        player.player.addMediaItem(i, mediaItems[i]);
                    for (i in newCurrentIndex + 1..<newQueue.size)
                        player.player.addMediaItem(i, mediaItems[i]);
                }
                onQueueChanged.emit(newQueue);
            }
        }
    }


    fun setQueuePlayNext(context: Context, playable: IPlayableTrack) {
        val currentPlaying = getCurrentTrack() ?: return;
        val currentQueue = getQueue().toMutableList();
        val index = currentQueue.indexOf(currentPlaying);
        if(index < 0)
            return;
        currentQueue.add(index + 1, playable);
        setQueueModify(context, currentQueue)
    }


    companion object {

        private val TAG = "StateQueue";
        @SuppressLint("StaticFieldLeak") //This is only alive while MainActivity is alive
        private var _instance : StateQueue? = null;
        val instance : StateQueue
            get(){
                if(_instance == null)
                    _instance = StateQueue();
                return _instance!!
            };

    }
}