package com.futo.music.ui.views.containers

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.futo.music.R
import com.futo.music.constructs.Event1
import com.futo.music.dp
import com.futo.music.ui.adapters.AnyAdapterView
import com.futo.music.ui.adapters.AnyAdapterView.Companion.asAny
import com.futo.music.ui.adapters.AnyInsertedAdapterView
import com.futo.music.ui.adapters.AnyInsertedAdapterView.Companion.asAnyWithViews
import com.futo.music.ui.base.RecyclerContainer
import com.futo.music.ui.buttons.StandardButton
import com.futo.music.ui.viewholders.ListPlaylistToggleViewHolder
import com.futo.music.ui.viewholders.ListPlaylistViewHolder
import kotlin.collections.arrayListOf

class PlaylistsToggleView(context: Context, addButtonText: String? = null, addButtonHandler: (()-> Unit)? = null): RecyclerContainer(context) {

    val onPlaylistToggleChanged = Event1<ListPlaylistToggleViewHolder.Item>();

    val adapter: AnyInsertedAdapterView<ListPlaylistToggleViewHolder.Item, ListPlaylistToggleViewHolder> =
        _recycler.asAnyWithViews<ListPlaylistToggleViewHolder.Item, ListPlaylistToggleViewHolder>(
            arrayListOf<View>(),
            arrayListOf<View>(*listOf(
                View(context).apply { this.minimumHeight = 20.dp(resources) },
                if(addButtonText != null && addButtonHandler != null)
                    StandardButton(context, null)
                        .withIcon(R.drawable.ic_add)
                        .withText(addButtonText)
                        .withBackground(R.drawable.background_button_accent)
                        .withOnClick {  addButtonHandler.invoke() }
                else null
            ).filterNotNull().toTypedArray()
            ),
            RecyclerView.VERTICAL, false, {
                it.onToggleChange.subscribe { holder, item ->
                    onPlaylistToggleChanged.emit(item);
                }
            })


    fun setPlaylists(items: List<ListPlaylistToggleViewHolder.Item>) {
        adapter.setData(items);
    }
}