package com.futo.music.fragments.main

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.futo.music.BuildConfig
import com.futo.music.R
import com.futo.music.UIDialogs
import com.futo.music.UIDialogs.ActionStyle
import com.futo.music.activities.MainActivity
import com.futo.music.extensions.assume
import com.futo.music.fragments.MainFragView
import com.futo.music.fragments.top.GeneralTopBarFragment
import com.futo.music.models.ImageVariable
import com.futo.music.models.playable.IPlayable
import com.futo.music.models.playable.Vibe
import com.futo.music.openPlayable
import com.futo.music.states.StateApp
import com.futo.music.states.StateDatabase
import com.futo.music.states.StateLibrary
import com.futo.music.storage.db.DBAlbum
import com.futo.music.storage.db.DBArtist
import com.futo.music.storage.db.DBPlaylist
import com.futo.music.ui.buttons.RoundButton
import com.futo.music.ui.buttons.StandardButton
import com.futo.music.ui.views.containers.ContentGrid
import com.futo.music.ui.views.general.SearchBarView
import com.futo.music.ui.views.playback.PlayableOptionOverlay
import com.futo.music.ui.views.topbars.GeneralTopBarView
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
    private var _dataSongNew: List<IPlayable>? = null;
    private var _dataVibeWeighted: Vibe? = null;

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

    override fun onShown(parameter: Any?, isBack: Boolean) {
        super.onShown(parameter, isBack);
    }
    override fun onHide() {
        super.onHide();
        _view?.onHide();
    }

    override fun onCreateMainView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = FragView(this, inflater);
        _view = view;
        StateLibrary.instance.onSyncCompleted.subscribe("homeFrag", {
            clearCache();
            lifecycleScope.launch(Dispatchers.Main) {
                _view?.updateContent();
            }
        });
        return view;
    }
    override fun onDestroyMainView() {
        super.onDestroyMainView();
        StateLibrary.instance.onSyncCompleted.remove("homeFrag");
        _view = null;
    }


    class FragView(frag: HomeFragment, inflater: LayoutInflater): MainFragView<HomeFragment>(frag, inflater, R.layout.fragment_home) {

        val search: SearchBarView;
        val gridRecent: ContentGrid;
        val gridPlaylists: ContentGrid;
        val gridArtists: ContentGrid;
        val gridAlbums: ContentGrid;
        val gridSongRecent: ContentGrid;

        val containerPlaylistsCreate: ConstraintLayout;
        val buttonPlaylistsCreate: RoundButton;

        val containerShuffles: LinearLayout;
        val buttonShuffle: StandardButton;
        val buttonWshuffle: StandardButton;


        init {
            search = findViewById(R.id.view_search);
            gridRecent = findViewById(R.id.grid_recent);
            gridPlaylists = findViewById(R.id.grid_playlists);
            gridArtists = findViewById(R.id.grid_artists);
            gridAlbums = findViewById(R.id.grid_albums);
            gridSongRecent = findViewById(R.id.grid_songs_recent);

            containerShuffles = findViewById(R.id.container_shuffles);
            buttonShuffle = findViewById(R.id.button_shuffle);
            buttonWshuffle = findViewById(R.id.button_wshuffle);

            findViewById<GeneralTopBarView>(R.id.topbar).apply {
                setTitleLongPress {
                    UIDialogs.showDialogVertical(context, 0, false, "Hidden Menu", "Some hidden options for testing", null, null, null, -1,
                        UIDialogs.Action("Rescan", {
                            StateApp.instance.activity()?.sync(true)
                        }, ActionStyle.PRIMARY));
                }
                setFragment(fragment);
                setTitleMini($"(v${BuildConfig.VERSION_CODE})")
            }

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
                it.openPlayable(fragment);
            }
            gridArtists.onLongClick.subscribe {
                it.openPlayable(fragment, true);
            }
            gridAlbums.onClick.subscribe {
                it.openPlayable(fragment);
            }
            gridAlbums.onLongClick.subscribe {
                it.openPlayable(fragment, true);
            }
            gridPlaylists.onClick.subscribe {
                it.openPlayable(fragment);
            }
            gridPlaylists.onLongClick.subscribe {
                it.openPlayable(fragment, true);
            }
            gridRecent.onClick.subscribe {
                it.openPlayable(fragment);
            }
            gridRecent.onLongClick.subscribe {
                it.openPlayable(fragment, true);
            }
            gridSongRecent.onClick.subscribe {
                it.openPlayable(fragment);
            }
            gridSongRecent.onLongClick.subscribe {
                it.openPlayable(fragment, true);
            }

            buttonPlaylistsCreate.onClick.subscribe {
                showNewPlaylistDialog();
            }


            search.onFocusChange.subscribe {
                if(it) {
                    fragment.navigate<SearchFragment>();
                }
            }

            buttonShuffle.onClick.subscribe {
                fragment.lifecycleScope.launch(Dispatchers.IO) {
                    val tracks = StateDatabase.instance.getTrackListShuffled(500);
                    withContext(Dispatchers.Main) {
                        fragment.navigate<PlaybackFragment>(Vibe("Shuffle", ImageVariable.fromResource(R.drawable.unknown_music), listOf(), listOf(), tracks));
                    }
                }
            }
            buttonWshuffle.onClick.subscribe {
                fragment.lifecycleScope.launch(Dispatchers.IO) {
                    val tracks = StateDatabase.instance.getTrackListWeighted(500);
                    withContext(Dispatchers.Main) {
                        fragment.navigate<PlaybackFragment>(Vibe("Weighted", ImageVariable.fromResource(R.drawable.unknown_music), listOf(), listOf(), tracks));
                    }
                }
            }

            updateContent();
        }

        fun showNewPlaylistDialog() {
            UIDialogs.showCreatePlaylistDialog(context, fragment.lifecycleScope) {
                fragment.clearCache();
                updateContent();
            };
        }

        fun updateContent() {
            fragment.lifecycleScope.launch(Dispatchers.IO) {
                val recent = fragment._dataRecent ?: StateDatabase.instance.getRecentPlays();
                val artists = fragment._dataArtists ?: StateDatabase.instance.getArtistsByRecent();
                val albums = fragment._dataAlbums ?:  StateDatabase.instance.getAlbumsByRecent();
                var playlists = (fragment._dataPlaylists ?: StateDatabase.instance.getPlaylistsByRecent()).map { it as IPlayable };
                val songNew = fragment._dataSongNew ?: StateDatabase.instance.getTracksNew(20);


                //val vibeWeighted = fragment._dataVibeWeighted ?: Vibe("Suggested", ImageVariable.fromResource(R.drawable.ic_playlist), listOf(), listOf(), StateDatabase.instance.getTrackListWeighted(100));

                //fragment._dataRecent = recent;
                fragment._dataArtists = artists;
                fragment._dataAlbums = albums;
                fragment._dataSongNew = songNew;
                //fragment._dataVibeWeighted = vibeWeighted;

                //if(vibeWeighted != null && vibeWeighted.singles.size > 5)
                //    playlists = listOf(vibeWeighted) + (playlists);

                //fragment._dataPlaylists = playlists;
                withContext(Dispatchers.Main) {

                    if(recent.any() || artists.any() || albums.any())
                        containerShuffles.isVisible = true;
                    else
                        containerShuffles.isVisible = false;

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

                    if(songNew.isNullOrEmpty())
                        gridSongRecent.visibility = View.GONE;
                    else {
                        gridSongRecent.setData(songNew);
                        gridSongRecent.visibility = View.VISIBLE;
                    }
                }
            }

            //val artists = StateLibrary.instance.getArtists(fragment.requireContext(), ArtistOrdering.TrackCount).map { it.toArtist() } as List<IPlayable>;
            //val albums = StateLibrary.instance.getAlbums(fragment.requireContext()).map { it.toAlbum() } as List<IPlayable>;

        }

        fun onShown(paramter: Any? = null) {
            //StateApp.instance.activity()?.setBackgroundTopGradient(Color.rgb(10, 9, 39), 2f, 1f)
            StateApp.instance.activity()?.setBackgroundTop(resources.getDrawable(R.drawable.background_glow), 2.5f, 0.5f);
        }

        fun onHide() {
        }
    }
}