package com.futo.music.fragments.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.futo.music.R
import com.futo.music.UIDialogs
import com.futo.music.fragments.MainFragView
import com.futo.music.models.playable.IPlayable
import com.futo.music.states.StateDatabase
import com.futo.music.states.StateLibrary
import com.futo.music.storage.db.DBAlbum
import com.futo.music.storage.db.DBArtist
import com.futo.music.storage.db.DBPlaylist
import com.futo.music.ui.buttons.RoundButton
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
    private var _dataPlaylists: List<DBPlaylist>? = null;

    fun clearCache() {
        _dataAlbums = null;
        _dataArtists = null;
        _dataRecent = null;
        _dataPlaylists = null;
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);

        StateDatabase.instance.onLibraryUpdated.subscribe(this) {
            clearCache();
            lifecycleScope.launch(Dispatchers.Main) {
                _view?.updateContent();
            }
        }

    }

    override fun onDestroy() {
        StateDatabase.instance.onLibraryUpdated.remove(this);
        super.onDestroy()
    }

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

        val containerPlaylistsCreate: ConstraintLayout;
        val buttonPlaylistsCreate: RoundButton;


        init {
            search = findViewById(R.id.view_search);
            gridRecent = findViewById(R.id.grid_recent);
            gridPlaylists = findViewById(R.id.grid_playlists);
            gridArtists = findViewById(R.id.grid_artists);
            gridAlbums = findViewById(R.id.grid_albums);

            containerPlaylistsCreate = findViewById(R.id.container_playlist_create);
            buttonPlaylistsCreate = findViewById(R.id.button_add_playlist);

            gridArtists.setButtonListener {
                fragment._dataArtists?.let {
                    fragment.navigate<ContentsFragment>(Pair("Artists", it));
                }
            }
            gridAlbums.setButtonListener {
                fragment._dataAlbums?.let {
                    fragment.navigate<ContentsFragment>(Pair("Albums", it));
                }
            }
            gridPlaylists.setButtonListener(androidx.media3.session.R.drawable.media3_icon_plus) {
                showNewPlaylistDialog();
            }

            gridArtists.onClick.subscribe {
                fragment.navigate<PlaybackFragment>(it);
            }
            gridAlbums.onClick.subscribe {
                fragment.navigate<PlaybackFragment>(it);
            }
            gridPlaylists.onClick.subscribe {
                fragment.navigate<PlaybackFragment>(it);
            }
            gridRecent.onClick.subscribe {
                fragment.navigate<PlaybackFragment>(it);
            }


            buttonPlaylistsCreate.onClick.subscribe {
                showNewPlaylistDialog();
            }


            search.onFocusChange.subscribe {
                if(it) {
                    fragment.navigate<SearchFragment>();
                }
            }


            updateContent();
        }

        fun showNewPlaylistDialog() {
            val dialog = UIDialogs.showDialog(context, R.drawable.ic_playlist, false, "New Playlist", "Enter a name for your new playlist", null, "", "Playlist name...", 0,
                UIDialogs.Action("Cancel", {

                }, UIDialogs.ActionStyle.NONE, true),
                UIDialogs.Action.withInput("Create", { result ->

                    if(result?.text.isNullOrBlank()) {
                        UIDialogs.appToast("No name provided for playlist");
                        return@withInput;
                    }
                    fragment.lifecycleScope.launch(Dispatchers.IO) {
                        StateDatabase.instance.createPlaylist(result.text);

                        withContext(Dispatchers.Main) {
                            fragment.clearCache();
                            updateContent();
                        }
                    }
                }, UIDialogs.ActionStyle.PRIMARY, true));
        }

        fun updateContent() {
            fragment.lifecycleScope.launch(Dispatchers.IO) {
                val recent = fragment._dataRecent ?: StateDatabase.instance.getRecentPlays();
                val artists = fragment._dataArtists ?: StateDatabase.instance.getArtistsByRecent();
                val albums = fragment._dataAlbums ?:  StateDatabase.instance.getAlbumsByRecent();
                val playlists = fragment._dataPlaylists ?: StateDatabase.instance.getPlaylistsByRecent();
                //fragment._dataRecent = recent;
                fragment._dataArtists = artists;
                fragment._dataAlbums = albums;
                //fragment._dataPlaylists = playlists;
                withContext(Dispatchers.Main) {
                    if(recent.isNullOrEmpty())
                        gridRecent.visibility = View.GONE;
                    else {
                        gridRecent.setData(recent);
                        gridRecent.visibility = View.VISIBLE;
                    }
                    if(playlists.isNullOrEmpty()) {
                        gridPlaylists.visibility = View.GONE;
                        containerPlaylistsCreate.visibility = View.VISIBLE;
                    }
                    else {
                        gridPlaylists.setData(playlists);
                        gridPlaylists.visibility = View.VISIBLE;
                        containerPlaylistsCreate.visibility = View.GONE;
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

            StateLibrary.instance.onSyncCompleted.subscribe("homeFrag", {
                fragment.lifecycleScope.launch(Dispatchers.Main) {
                    updateContent();
                }
            });
        }

        fun onHide() {
            StateLibrary.instance.onSyncCompleted.remove("homeFrag");
        }
    }
}