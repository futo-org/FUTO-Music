package com.futo.music.fragments.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.collection.emptyLongSet
import androidx.compose.animation.core.updateTransition
import androidx.lifecycle.lifecycleScope
import com.futo.music.R
import com.futo.music.fragments.MainFragView
import com.futo.music.models.playable.IPlayable
import com.futo.music.states.ArtistOrdering
import com.futo.music.states.StateDatabase
import com.futo.music.states.StateLibrary
import com.futo.music.storage.db.DBAlbum
import com.futo.music.storage.db.DBArtist
import com.futo.music.ui.views.containers.ContentGrid
import com.futo.music.ui.views.general.SearchBarView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment: MainFragment() {
    override val isMainView : Boolean = true;
    override val isTab: Boolean = true;
    override val hasBottomBar: Boolean get() = true;

    override val fragmentTitle: String = "Home";

    private var _view: FragView? = null;

    private var _dataAlbums: List<DBAlbum>? = null;
    private var _dataArtists: List<DBArtist>? = null;
    private var _dataRecent: List<IPlayable>? = null;


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


    class FragView(frag: HomeFragment, inflater: LayoutInflater): MainFragView<HomeFragment>(frag, inflater, R.layout.fragment_home) {

        val search: SearchBarView;
        val gridRecent: ContentGrid;
        val gridPlaylists: ContentGrid;
        val gridArtists: ContentGrid;
        val gridAlbums: ContentGrid;


        init {
            search = findViewById(R.id.view_search);
            gridRecent = findViewById(R.id.grid_recent);
            gridPlaylists = findViewById(R.id.grid_playlists);
            gridArtists = findViewById(R.id.grid_artists);
            gridAlbums = findViewById(R.id.grid_albums);

            gridArtists.setButtonListListener {
                fragment._dataArtists?.let {
                    fragment.navigate<ContentsFragment>(Pair("Artists", it));
                }
            }
            gridAlbums.setButtonListListener {
                fragment._dataAlbums?.let {
                    fragment.navigate<ContentsFragment>(Pair("Albums", it));
                }
            }

            gridArtists.onClick.subscribe {
                fragment.navigate<PlaybackFragment>(it);
            }
            gridAlbums.onClick.subscribe {
                fragment.navigate<PlaybackFragment>(it);
            }

            search.onFocusChange.subscribe {
                if(it) {
                    fragment.navigate<SearchFragment>();
                }
            }


            updateContent();
        }

        fun updateContent() {
            val playlists = listOf<IPlayable>();

            fragment.lifecycleScope.launch(Dispatchers.IO) {
                val recent = fragment._dataRecent ?: StateDatabase.instance.getRecentPlays();
                val artists = fragment._dataArtists ?: StateDatabase.instance.getArtistsByRecent();
                val albums = fragment._dataAlbums ?:  StateDatabase.instance.getAlbumsByRecent();
                fragment._dataRecent = recent;
                fragment._dataArtists = artists;
                fragment._dataAlbums = albums;
                withContext(Dispatchers.Main) {
                    if(recent.isNullOrEmpty())
                        gridRecent.visibility = View.GONE;
                    else {
                        gridRecent.setData(recent);
                        gridRecent.visibility = View.VISIBLE;
                    }
                    if(playlists.isNullOrEmpty())
                        gridPlaylists.visibility = View.GONE;
                    else {
                        gridPlaylists.setData(playlists);
                        gridPlaylists.visibility = View.VISIBLE;
                    }
                    if(artists.isNullOrEmpty())
                        gridArtists.visibility = View.GONE;
                    else {
                        gridArtists.setData(artists);
                        gridArtists.visibility = View.VISIBLE;
                    }
                    if(albums.isNullOrEmpty())
                        gridAlbums.visibility = View.GONE;
                    else {
                        gridAlbums.setData(albums);
                        gridAlbums.visibility = View.VISIBLE;
                    }
                }
            }

            //val artists = StateLibrary.instance.getArtists(fragment.requireContext(), ArtistOrdering.TrackCount).map { it.toArtist() } as List<IPlayable>;
            //val albums = StateLibrary.instance.getAlbums(fragment.requireContext()).map { it.toAlbum() } as List<IPlayable>;

        }

        fun onShown(paramter: Any? = null) {

        }

        fun onHide() {

        }
    }
}