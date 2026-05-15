package com.futo.music.fragments.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.futo.music.PlaySettings
import com.futo.music.R
import com.futo.music.dp
import com.futo.music.fragments.MainFragView
import com.futo.music.fragments.top.NavigationTopBarFragment
import com.futo.music.models.playable.IPlayableTrack
import com.futo.music.openPlayable
import com.futo.music.setHeaderScrollFade
import com.futo.music.states.StateDatabase
import com.futo.music.storage.db.DBPlaylist
import com.futo.music.storage.db.DBTrack
import com.futo.music.ui.adapters.AnyInsertedAdapterView
import com.futo.music.ui.adapters.AnyInsertedAdapterView.Companion.asAnyWithViews
import com.futo.music.ui.adapters.TrackAnyViewHolder
import com.futo.music.ui.views.NoResultsView
import com.futo.music.ui.views.containers.PlayableHeader
import com.futo.music.withSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaylistFragment: MainFragment() {
    override val isMainView : Boolean = true
    override val isTab: Boolean = true
    override val hasBottomBar: Boolean get() = true

    override val fragmentTitle: String = "Playlist"

    private var _view: FragView? = null

    override fun onShownWithView(parameter: Any?, isBack: Boolean) {
        super.onShownWithView(parameter, isBack)
        _view?.onShown(parameter)
    }

    override fun onHide() {
        super.onHide()
        _view?.onHide()
    }

    override fun onCreateMainView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = FragView(this, inflater)
        _view = view
        return view
    }

    override fun onDestroyMainView() {
        super.onDestroyMainView()
        _view = null
    }

    class FragView(frag: PlaylistFragment, inflater: LayoutInflater): MainFragView<PlaylistFragment>(frag, inflater, R.layout.fragment_album) {

        val root: ConstraintLayout
        val recycler: RecyclerView
        val header: PlayableHeader
        val emptyView: NoResultsView

        var playlistCurrent: DBPlaylist? = null

        var containerTop: ConstraintLayout
        val buttonBack: ImageButton

        private val _containerButtons: LinearLayout
        private val _adapter: AnyInsertedAdapterView<IPlayableTrack, TrackAnyViewHolder>
        private var _adapterDataset: List<IPlayableTrack>? = null

        init {
            root = findViewById(R.id.root)
            recycler = findViewById(R.id.recycler)
            header = PlayableHeader(context)
            emptyView = NoResultsView(context)

            containerTop = findViewById(R.id.container_top)
            buttonBack = findViewById(R.id.button_back)
            _containerButtons = findViewById(R.id.container_buttons)

            header.onPlayAll.subscribe {
                playlistCurrent?.let {
                    fragment.navigate<PlaybackFragment>(it)
                }
            }
            header.onShuffleAll.subscribe {
                playlistCurrent?.let {
                    fragment.navigate<PlaybackFragment>(it.withSettings(PlaySettings(shuffle = true)))
                }
            }

            _adapter = recycler.asAnyWithViews<IPlayableTrack, TrackAnyViewHolder>(arrayListOf(header), arrayListOf(emptyView), RecyclerView.VERTICAL, false) {
                it.useFullName = true
                it.onClick.subscribe {
                    if (it is DBTrack)
                        it.openPlayable(fragment)
                }
                it.onLongClick.subscribe {
                    if (it is DBTrack)
                        it.openPlayable(fragment, true)
                }
            }

            val fadeOffset = 30.dp(resources)
            recycler.setHeaderScrollFade(containerTop, fadeOffset) {
                // no-op for now
            }

            header.onSearchChanged.subscribe { q ->
                if (q.isBlank())
                    _adapter.setData(_adapterDataset ?: return@subscribe)
                else
                    _adapter.setData(_adapterDataset?.filter { if (it is DBTrack) it.filter(q) else true } ?: return@subscribe)
            }
            buttonBack.setOnClickListener {
                fragment.closeSegment()
            }
        }

        fun updateContent(playlistId: Long) {
            findViewTreeLifecycleOwner()?.lifecycleScope?.launch(Dispatchers.IO) {
                val playlist = StateDatabase.instance.getPlaylist(playlistId) ?: return@launch
                withContext(Dispatchers.Main) {
                    updateContent(playlist)
                }
            }
        }

        fun updateContent(playlist: DBPlaylist) {
            playlistCurrent = playlist

            header.setPlayable(playlist, true)
            header.setMetadata("")
            header.clearSearch()
            fragment.topBar?.let {
                if (it is NavigationTopBarFragment) {
                    it.setTitle(playlist.name)
                }
            }

            fragment.lifecycleScope.launch(Dispatchers.IO) {
                val songs = StateDatabase.instance.getPlaylistTracks(playlist.id)

                withContext(Dispatchers.Main) {
                    _adapter.setData(songs)
                    _adapterDataset = songs

                    header.setMetadata("${songs.size} track" + (if (songs.size > 1) "s" else ""))

                    emptyView.isVisible = songs.isEmpty()
                }
            }
        }

        fun onShown(parameter: Any? = null) {
            if (parameter is DBPlaylist)
                updateContent(parameter)
        }

        fun onHide() {

        }
    }
}
