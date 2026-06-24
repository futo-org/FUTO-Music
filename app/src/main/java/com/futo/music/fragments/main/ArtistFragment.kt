package com.futo.music.fragments.main

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
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
import com.futo.music.storage.db.DBAlbum
import com.futo.music.storage.db.DBArtist
import com.futo.music.storage.db.DBTrack
import com.futo.music.ui.adapters.AnyInsertedAdapterView
import com.futo.music.ui.adapters.AnyInsertedAdapterView.Companion.asAnyWithViews
import com.futo.music.ui.adapters.TrackAnyViewHolder
import com.futo.music.ui.views.NoResultsView
import com.futo.music.ui.views.containers.ContentGrid
import com.futo.music.ui.views.containers.PlayableHeader
import com.futo.music.withSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ArtistFragment: MainFragment() {
    override val isMainView : Boolean = true;
    override val isTab: Boolean = true;
    override val hasBottomBar: Boolean get() = true;

    override val fragmentTitle: String = "Album";

    private var _view: FragView? = null;


    override fun onShownWithView(parameter: Any?, isBack: Boolean) {
        super.onShownWithView(parameter, isBack);
        _view?.onShown(parameter);
    }

    override fun onHide() {
        super.onHide();
        _view?.onHide();
    }

    override fun onCreateMainView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = FragView(this, inflater);
        _view = view;
        return view;
    }
    override fun onDestroyMainView() {
        super.onDestroyMainView();
        _view = null;
    }


    class FragView(frag: ArtistFragment, inflater: LayoutInflater): MainFragView<ArtistFragment>(frag, inflater, R.layout.fragment_artist) {

        val root: ConstraintLayout;

        val recycler: RecyclerView;

        val header: PlayableHeader;
        val emptyView: NoResultsView;

        var artistCurrent: DBArtist? = null;

        var containerTop: ConstraintLayout;
        val buttonBack: ImageButton;

        private val gridAlbums: ContentGrid;

        private val _adapter: AnyInsertedAdapterView<IPlayableTrack, TrackAnyViewHolder>;
        private var _adapterDataset: List<IPlayableTrack>? = null;


        init {
            root = findViewById(R.id.root);
            recycler = findViewById(R.id.recycler)
            header = PlayableHeader(context);
            emptyView = NoResultsView(context);
            gridAlbums = ContentGrid(context, false, 150.dp(resources), "")
            gridAlbums.gridSettings.hideMetadata = true;
            gridAlbums.onClick.subscribe {
                it.openPlayable(fragment);
            }
            gridAlbums.onLongClick.subscribe {
                it.openPlayable(fragment, true);
            }

            containerTop = findViewById(R.id.container_top);
            buttonBack = findViewById(R.id.button_back);

            header.setAdditionalViews(listOf(gridAlbums));

            header.onPlayAll.subscribe {
                artistCurrent?.let {
                    fragment.navigate<PlaybackFragment>(it);
                }
            }
            header.onShuffleAll.subscribe {
                artistCurrent?.let {
                    fragment.navigate<PlaybackFragment>(it.withSettings(PlaySettings(shuffle = true)));
                }
            }

            _adapter = recycler.asAnyWithViews<IPlayableTrack, TrackAnyViewHolder>(arrayListOf<View>(header), arrayListOf<View>(emptyView), RecyclerView.VERTICAL, false, {
                it.useFullName = false;
                it.onClick.subscribe {
                    if(it is DBTrack)
                        it.openPlayable(fragment, parentPlayable = artistCurrent);
                }
                it.onLongClick.subscribe {
                    if(it is DBTrack)
                        it.openPlayable(fragment, true);
                }
            });

            val fadeOffset = 30.dp(resources);
            recycler.setHeaderScrollFade(containerTop, fadeOffset);

            header.onSearchChanged.subscribe { q -> //TODO: Implement efficient filtering on AnyAdapter
                if(q.isBlank())
                    _adapter.setData(_adapterDataset ?: return@subscribe);
                else
                    _adapter.setData(_adapterDataset?.filter { if(it is DBTrack) it.filter(q) else true } ?: return@subscribe);
            }
            buttonBack.setOnClickListener {
                fragment.closeSegment();
            }
        }

        fun updateContent(artistId: Long) {
            findViewTreeLifecycleOwner()?.lifecycleScope?.launch(Dispatchers.IO) {
                val artist = StateDatabase.instance.getArtist(artistId) ?: return@launch;
                withContext(Dispatchers.Main) {
                    updateContent(artist);
                }
            }
        }
        fun updateContent(artist: DBArtist) {
            artistCurrent = artist;

            header.setPlayable(artist, true);
            header.setMetadata("");
            header.clearSearch();
            fragment.topBar?.let {
                if(it is NavigationTopBarFragment) {
                    it.setTitle(artist.name)
                }
            }

            fragment.lifecycleScope.launch(Dispatchers.IO) {

                val songs = StateDatabase.instance.getArtistTracks(artist.id);
                val albums = StateDatabase.instance.getArtistAlbums(artist.id).sortedByDescending { it.datePlayed }

                withContext(Dispatchers.Main) {
                    gridAlbums.setData(albums);

                    _adapter.setData(songs);
                    _adapterDataset = songs;

                    if(albums.size > 0) {
                        header.setMetadata(("${songs.size} track" + (if(songs.size > 1 || songs.size == 0) "s" else "") + " · ${albums.size} album" + (if(albums.size > 1) "s" else "")));
                    }
                    else
                        header.setMetadata("${songs.size} track" + (if(songs.size > 1) "s" else ""));

                    if (songs.isEmpty())
                        emptyView.isVisible = true;
                    else
                        emptyView.isVisible = false;
                }
            }
        }

        fun onShown(parameter: Any? = null) {
            if(parameter is DBArtist)
                updateContent(parameter);
        }

        fun onHide() {

        }
    }
}