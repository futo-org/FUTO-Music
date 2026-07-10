package com.futo.music.ui.adapters

import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.futo.music.constructs.Event1
import com.futo.music.constructs.Event2
import com.futo.music.models.playable.IPlayable
import com.futo.music.models.playable.PlayableType
import com.futo.music.storage.db.DBTrack
import com.futo.music.ui.viewholders.FilesDirectoryViewHolder
import com.futo.music.ui.viewholders.FilesFileViewHolder
import com.futo.music.ui.viewholders.FilesRootViewHolder
import com.futo.music.ui.views.grid.ContentAlbumGridView
import com.futo.music.ui.views.grid.ContentArtistGridView
import com.futo.music.ui.views.grid.ContentTrackGridView
import com.futo.music.ui.views.grid.GridSettings
import com.futo.music.ui.views.grid.IContentGridView

class FilesAdapter(val onCreate: ((hold: FilesAdapter.ViewHolder)->Unit)?): RecyclerView.Adapter<FilesAdapter.ViewHolder>() {
    val data: MutableList<IFileItem> = mutableListOf();
    val dataAll: MutableList<IFileItem> = mutableListOf();
    var filter: String? = null;

    val onClick = Event1<IPlayable>();



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val result = when(viewType) {
            //FilesItemType.Unknown.value -> ViewHolder((parent))
            FilesItemType.Root.value -> ViewHolder(FilesRootViewHolder(parent))
            FilesItemType.Directory.value -> ViewHolder(FilesDirectoryViewHolder(parent))
            FilesItemType.File.value -> ViewHolder(FilesFileViewHolder(parent))
            else -> throw NotImplementedError();
        }
        onCreate?.invoke(result);
        return result;
    }

    override fun getItemViewType(position: Int): Int {
        val item = data.getOrNull(position);

        return item?.type?.value ?: FilesItemType.Unknown.value;
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data.getOrNull(position);
        if(item != null && holder.view is IFilesView) {
            holder.view.bind(item);
        }
    }

    override fun getItemCount(): Int {
        return data.size;
    }

    fun setData(items: List<IFileItem>) {
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
        val view: IFilesView;
        constructor(view: IFilesView): super(view.root) {
            this.view = view;
        }
    }
}

enum class FilesItemType(val value: Int) {
    Unknown(0),
    Directory(1),
    File(2),
    Root(3)
}

interface IFileItem {
    val type: FilesItemType;

    val name: String;
    val path: String;
}

interface IFilesView {

    val root: ConstraintLayout

    val onClick: Event2<IFilesView, IFileItem>;
    val onOptions: Event2<IFilesView, IFileItem>;

    fun bind(fileItem: IFileItem);
}