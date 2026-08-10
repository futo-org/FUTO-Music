package com.futo.music.states

import android.annotation.SuppressLint
import android.content.Context
import androidx.media3.common.MediaItem
import com.futo.music.constructs.Event1
import com.futo.music.logic.PlayerManager
import com.futo.music.models.playable.IPlayable
import com.futo.music.models.playable.IPlayableTrack
import com.futo.music.models.playable.PlayableDescriptor
import com.futo.music.models.playable.QueueType
import com.futo.music.models.playable.Vibe
import com.futo.music.services.PlaybackService
import com.futo.music.storage.db.DBAlbum
import com.futo.music.storage.db.DBArtist
import com.futo.music.storage.db.DBPlaylist
import com.futo.music.storage.db.DBTrack
import com.futo.music.storage.file.FragmentedStorage
import com.futo.music.storage.file.ManagedStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections
import kotlin.collections.map
import kotlin.text.toLongOrNull


class StateQueue {

    val _queue: MutableList<IPlayableTrack> = mutableListOf();
    val isQueueEmpty: Boolean get() = _queue.isEmpty();

    var queueType: QueueType = QueueType.Unknown
        private set;

    val onQueueChanged = Event1<List<IPlayableTrack>>();

    private var _player: PlayerManager? = null;

    private var _lastSetQueuePlayable: IPlayable? = null;
    private var _lastSetMediaItems: List<MediaItem>? = null;

    private var _reasons: List<String?>? = null;

    private val _lastSetQueuePlayableDescriptor: ManagedStore<PlayableDescriptor?> = FragmentedStorage.storeJson<PlayableDescriptor?>("lastPlayable").load();

    fun setPlayer(manager: PlayerManager) {
        _player = manager;
    }

    fun getRecommendationReason(queueIndex: Int): String? {
        return _reasons?.elementAtOrNull(queueIndex);
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
    fun setQueue(context: Context, playable: IPlayable, cb: ((List<IPlayableTrack>)->Unit)? = null, index: Int = -1) {
        StateApp.instance.scopeOrNull?.launch(Dispatchers.IO) {
            if(playable is DBAlbum)
                StateDatabase.instance.setPlayedAlbum(playable.id);
            else if(playable is DBArtist)
                StateDatabase.instance.setPlayedArtist(playable.id);
            else if(playable is DBTrack)
                StateDatabase.instance.setPlayedTrack(playable.id, true);
            else if(playable is DBPlaylist)
                StateDatabase.instance.setPlayedPlaylist(playable.id);

            val reasons = if(playable is Vibe)
                playable.recomReasons
            else null;

            val items = playable.getTracks(context);

            setPersistentQueue(playable, items)

            val newQueue: List<IPlayableTrack>;
            synchronized(_queue) {
                _queue.clear();
                _queue.addAll(items);
                _reasons = reasons;
                _lastSetQueuePlayable = playable;
                newQueue = _queue.toList();
            }

            if(playable is Vibe)
                queueType = playable.vibeType;
            else
                queueType = QueueType.Unknown;

            if(queueType == QueueType.Shuffle || queueType == QueueType.SmartShuffle) {
                withContext(Dispatchers.Main) {
                    _player?.player?.shuffleModeEnabled = false;
                }
            }

            val mediaItems = newQueue.map { it.getMediaItem() }
            setLastMediaItems(mediaItems);
            withContext(Dispatchers.Main) {
                if(index > 0)
                    _player?.player?.setMediaItems(mediaItems, index, 0);
                else
                    _player?.player?.setMediaItems(mediaItems);
                _player?.player?.prepare();
                _player?.player?.play();
            }
            onQueueChanged.emit(newQueue);
            cb?.invoke(newQueue);
        }
    }

    fun clearQueue() {
        synchronized(_queue) {
            _queue.clear();
            _reasons = null;
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
                StateDatabase.instance.setPlayedTrack(playable.id, true);
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

        val oldQueue = _queue.toMutableList();
        val newQueue: List<IPlayableTrack>;
        synchronized(_queue) {
            _queue.clear();
            _queue.addAll(items);
            _reasons = null;
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
                    var addedCount = 0;
                    for(newPos in 0..<newQueue.size) {
                        val item = newQueue[newPos];
                        val oldPos = oldQueue.indexOfFirst { it.getItemId() != null && it.getItemId() == item.getItemId() }
                        if(oldPos > -1) {
                            player.player.moveMediaItem(oldPos, newPos);
                            Collections.swap(oldQueue, oldPos, newPos);
                        }
                        else {
                            player.player.addMediaItem(newPos, mediaItems[newPos]);
                            oldQueue.add(newPos, item);
                            addedCount++;
                        }
                    }
                    /*
                    for (i in 0..<oldCurrentIndex)
                        player.player.removeMediaItem(i);
                    for (i in 1+oldCurrentIndex..<player.player.mediaItemCount - 1)
                        player.player.removeMediaItem(i);
                    for (i in 0..<newCurrentIndex)
                        player.player.addMediaItem(i, mediaItems[i]);
                    for (i in newCurrentIndex + 1..<newQueue.size)
                        player.player.addMediaItem(i, mediaItems[i]);
                    */
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

        val existingIndex = currentQueue.indexOfFirst { it.getItemId() == playable.getItemId() };
        if(existingIndex >= 0) {
            currentQueue.removeAt(existingIndex);
            currentQueue.add(index + 1, playable);
        }
        else
            currentQueue.add(index + 1, playable);
        setQueueModify(context, currentQueue)
    }
    fun setQueueAdd(context: Context, playable: IPlayableTrack) {
        val currentQueue = getQueue().toMutableList();
        currentQueue.add(playable);
        setQueueModify(context, currentQueue);
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