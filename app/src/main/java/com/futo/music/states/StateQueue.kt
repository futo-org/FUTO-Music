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
import androidx.media3.common.MediaItem
import com.futo.music.constructs.Event1
import com.futo.music.logging.Logger
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

    val onQueueChanged = Event1<List<IPlayableTrack>>();

    private var _lastSetQueuePlayable: IPlayable? = null;
    private var _lastSetMediaItems: List<MediaItem>? = null;

    private val _lastSetQueuePlayableDescriptor: ManagedStore<PlayableDescriptor?> = FragmentedStorage.storeJson<PlayableDescriptor?>("lastPlayable").load();


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


    fun setLastMediaItems(items: List<MediaItem>) {
        _lastSetMediaItems = items;
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
            onQueueChanged.emit(newQueue);
        }
    }

    fun setQueuePlayNext(context: Context, playable: IPlayable) {
        //TODO: Implement
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