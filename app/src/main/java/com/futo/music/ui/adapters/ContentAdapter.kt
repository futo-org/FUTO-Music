package com.futo.music.ui.adapters

import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import androidx.collection.emptyLongSet
import androidx.recyclerview.widget.RecyclerView
import com.futo.music.constructs.Event1
import com.futo.music.models.playable.Album
import com.futo.music.models.playable.Artist
import com.futo.music.models.playable.IPlayable
import com.futo.music.models.playable.PlayableType
import com.futo.music.storage.db.DBTrack
import com.futo.music.ui.views.grid.ContentAlbumGridView
import com.futo.music.ui.views.grid.ContentArtistGridView
import com.futo.music.ui.views.grid.ContentPlaylistGridView
import com.futo.music.ui.views.grid.ContentTrackGridView
import com.futo.music.ui.views.grid.GridSettings
import com.futo.music.ui.views.grid.IContentGridView

class ContentAdapter(val onCreate: ((hold: ContentAdapter.ViewHolder)->Unit)?): RecyclerView.Adapter<ContentAdapter.ViewHolder>() {
    val data: MutableList<IPlayable> = mutableListOf();
    val dataAll: MutableList<IPlayable> = mutableListOf();
    var filter: String? = null;

    val onClick = Event1<IPlayable>();

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val result = when(viewType) {
            PlayableType.Track.value -> ViewHolder(ContentTrackGridView(parent))
            PlayableType.Artist.value -> ViewHolder(ContentArtistGridView(parent))
            PlayableType.Album.value -> ViewHolder(ContentAlbumGridView(parent))
            PlayableType.Playlist.value -> ViewHolder(ContentPlaylistGridView(parent))
            PlayableType.Vibe.value -> ViewHolder(ContentAlbumGridView(parent))
            else -> throw NotImplementedError();
        }
        onCreate?.invoke(result);
        return result;
    }

    override fun getItemViewType(position: Int): Int {
        val item = data.getOrNull(position);

        return item?.type?.value ?: PlayableType.Unknown.value;
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data.getOrNull(position);
        if(item != null && holder.view is IContentGridView) {
            holder.view.bind(item);
        }
    }

    override fun getItemCount(): Int {
        return data.size;
    }

    fun setData(items: List<IPlayable>) {
        data.clear();
        data.addAll(items);
        dataAll.clear();
        dataAll.addAll(items);

        val currentFilter = filter;
        if(currentFilter != null)
            search(currentFilter);
        else
            notifyDataSetChanged();
    }

    fun clearSearch() {
        filter = null;
        if(data.size != dataAll.size) {
            data.clear();
            data.addAll(dataAll);
            notifyDataSetChanged();
        }
    }
    fun search(str: String) { //TODO: Optimize
        filter = str;
        val query = str.lowercase().trim();
        val newList = dataAll.filter { it.name.lowercase().contains(query) || (it is DBTrack && it.artistLine != null && it.artistLine.contains(query)) };
        data.clear();
        data.addAll(newList);
        notifyDataSetChanged()
    }

    class ViewHolder: RecyclerView.ViewHolder {
        val view: IContentGridView;
        constructor(view: IContentGridView): super(view.root) {
            this.view = view;
        }

        fun setSize(width: Int, height: Int) {
            view.setSize(width, height);
        }
        fun setSettings(settings: GridSettings) {
            view.setSettings(settings);
        }
    }
}