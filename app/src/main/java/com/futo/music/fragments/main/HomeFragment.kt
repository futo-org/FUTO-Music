package com.futo.music.fragments.main

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.futo.music.BuildConfig
import com.futo.music.R
import com.futo.music.UIDialogs
import com.futo.music.fragments.MainFragView
import com.futo.music.logic.shuffles.ESmartShuffle
import com.futo.music.models.ImageVariable
import com.futo.music.models.playable.IPlayable
import com.futo.music.models.playable.Vibe
import com.futo.music.models.playable.QueueType
import com.futo.music.openPlayable
import com.futo.music.settings.Settings
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
import com.futo.music.ui.views.general.SortDropdownType
import com.futo.music.ui.views.topbars.GeneralTopBarView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime

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
    private var _dataUnrated: List<IPlayable>? = null;
    private var _dataMostPlayed: List<IPlayable>? = null;
    private var _dataSongNew: List<IPlayable>? = null;
    private var _dataVibeWeighted: Vibe? = null;
    private var _dataCacheTime: OffsetDateTime? = null;

    var _scrollY: Int? = null;
    var _recentStateSave: Parcelable? = null;
    var _playlistStateSave: Parcelable? = null;
    var _artistStateSave: Parcelable? = null;
    var _albumStateSave: Parcelable? = null;
    var _unratedStateSave: Parcelable? = null;
    var _songNewStateSave: Parcelable? = null;
    var _mostPlayedStateSave: Parcelable? = null;
    var _globalStateSave: Parcelable? = null;

    fun clearCache() {
        _dataAlbums = null;
        _dataArtists = null;
        _dataRecent = null;
        _dataPlaylists = null;
        _dataSongNew = null;
        _dataUnrated = null;
        _dataMostPlayed = null;
    }
    fun clearPlaylistsCache() {
        _dataPlaylists = null;
    }
    fun clearUnratedCache() {
        _dataUnrated = null;
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
        val prevView = _view;
        if(prevView != null) //Temporary solution
            return prevView;
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

        _recentStateSave = _view?.gridRecent?.recycler?.layoutManager?.onSaveInstanceState();
        _playlistStateSave = _view?.gridPlaylists?.recycler?.layoutManager?.onSaveInstanceState();
        _artistStateSave = _view?.gridArtists?.recycler?.layoutManager?.onSaveInstanceState();
        _albumStateSave = _view?.gridAlbums?.recycler?.layoutManager?.onSaveInstanceState();
        _unratedStateSave = _view?.gridUnrated?.recycler?.layoutManager?.onSaveInstanceState();
        _songNewStateSave = _view?.gridSongRecent?.recycler?.layoutManager?.onSaveInstanceState();
        _mostPlayedStateSave = _view?.gridMostPlayed?.recycler?.layoutManager?.onSaveInstanceState();
        _scrollY = _view?.scroller?.scrollY;
        _view = null;
    }


    class FragView(frag: HomeFragment, inflater: LayoutInflater): MainFragView<HomeFragment>(frag, inflater, R.layout.fragment_home) {

        val scroller: ScrollView;
        val search: SearchBarView;
        val gridRecent: ContentGrid;
        val gridPlaylists: ContentGrid;
        val gridArtists: ContentGrid;
        val gridAlbums: ContentGrid;
        val gridSongRecent: ContentGrid;
        val gridUnrated: ContentGrid;
        val gridMostPlayed: ContentGrid;

        val containerPlaylistsCreate: ConstraintLayout;
        val buttonPlaylistsCreate: RoundButton;

        val containerShuffles: LinearLayout;
        val buttonShuffle: StandardButton;
        val buttonWshuffle: StandardButton;
        val buttonHelp: StandardButton;


        init {
            search = findViewById(R.id.view_search);
            scroller = findViewById(R.id.scroller);
            gridRecent = findViewById(R.id.grid_recent);
            gridPlaylists = findViewById(R.id.grid_playlists);
            gridArtists = findViewById(R.id.grid_artists);
            gridAlbums = findViewById(R.id.grid_albums);
            gridUnrated = findViewById(R.id.grid_unrated);
            gridMostPlayed = findViewById(R.id.grid_most_played);
            gridSongRecent = findViewById(R.id.grid_songs_recent);

            containerShuffles = findViewById(R.id.container_shuffles);
            buttonShuffle = findViewById(R.id.button_shuffle);
            buttonWshuffle = findViewById(R.id.button_wshuffle);
            buttonHelp = findViewById(R.id.button_help);

            gridMostPlayed.gridSettings.showPlays = true;

            findViewById<GeneralTopBarView>(R.id.topbar).apply {
                setTitlePress {
                    StateApp.instance.activity()?.let {
                        it.showAlphaNotice()
                    }
                }

                setFragment(fragment);
                setTitleMini($"(v${BuildConfig.VERSION_CODE})")
            }

            containerPlaylistsCreate = findViewById(R.id.container_playlist_create);
            buttonPlaylistsCreate = findViewById(R.id.button_add_playlist);

            gridArtists.setButtonListener {
                fragment._dataArtists?.let {
                    fragment.navigate<ContentsFragment>(ContentsFragment.Parameters(it, "Artists"));
                }
            }
            gridAlbums.setButtonListener {
                fragment._dataAlbums?.let {
                    fragment.navigate<ContentsFragment>(ContentsFragment.Parameters(it, "Albums"));
                }
            }
            gridPlaylists.setButtonListener(androidx.media3.session.R.drawable.media3_icon_plus) {
                showNewPlaylistDialog();
            }
            gridSongRecent.setButtonListener {
                fragment.lifecycleScope.launch(Dispatchers.IO) {
                    val count = StateDatabase.instance.getTrackCount()
                    val allSongs = StateDatabase.instance.getAllTracks();
                    withContext(Dispatchers.Main) {
                        fragment.navigate<ContentsFragment>(ContentsFragment.Parameters(allSongs, "Songs",
                            sortOrder = SortDropdownType.AddedDesc));
                    }
                }
            }
            gridUnrated.onClick.subscribe {
                fragment.clearUnratedCache();
                it.openPlayable(fragment, true);
            }
            gridUnrated.setButtonListener {
                fragment.navigate<RatingsListFragment>();
            }
            gridMostPlayed.onClick.subscribe {
                it.openPlayable(fragment);
            }
            gridMostPlayed.setButtonListener {
                fragment.lifecycleScope.launch(Dispatchers.IO) {
                    val mostPlayed = StateDatabase.instance.getMostPlayed(20);
                    withContext(Dispatchers.Main) {
                        fragment.navigate<ContentsFragment>(ContentsFragment.Parameters(
                            mostPlayed,
                            "Most Played",
                            listOf(ContentsFragment.FLAG_MOST_PLAYED, ContentsFragment.FLAG_LIST),
                            SortDropdownType.PlaysDesc));
                    }
                }
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
            gridRecent.setButtonListener {
                fragment.lifecycleScope.launch(Dispatchers.IO) {
                    val allSongs = StateDatabase.instance.getAllTracks();
                    withContext(Dispatchers.Main) {
                        fragment.navigate<ContentsFragment>(ContentsFragment.Parameters(allSongs, "All Songs", listOf(), sortOrder = SortDropdownType.PlayedDesc));
                    }
                }
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
                        fragment.navigate<PlaybackFragment>(Vibe("Shuffle", ImageVariable.fromResource(R.drawable.unknown_music), listOf(), listOf(), tracks, QueueType.Shuffle));
                    }
                }
            }

            buttonWshuffle.onClick.subscribe {
                fragment.lifecycleScope.launch(Dispatchers.IO) {

                    val scores = StateDatabase.instance.getScoresContainer();
                    val shuffle = ESmartShuffle(scores);

                    val tracks = shuffle.getTracksWithReason(500);
                    withContext(Dispatchers.Main) {
                        fragment.navigate<PlaybackFragment>(Vibe("Smart Shuffle", ImageVariable.fromResource(R.drawable.unknown_music), listOf(), listOf(), tracks.first, QueueType.SmartShuffle, reasons = tracks.second));
                    }
                }
            }
            fragment.lifecycleScope.launch(Dispatchers.IO) {
                val tracks = StateDatabase.instance.getTrackListWeighted(10);
                if(tracks.size < 10) {
                    withContext(Dispatchers.Main) {
                        buttonWshuffle.onClick.clear();
                        buttonWshuffle.alpha = 0.5f;
                        buttonWshuffle.onClick.subscribe {
                            UIDialogs.appToast("Rate more songs before using Smart Shuffle.");
                        }
                    }
                }
            }

            buttonHelp.onClick.subscribe {
                UIDialogs.showGuideDialog(context, fragment.lifecycleScope, listOf(
                    UIDialogs.Companion.GuideItem("Shuffle", "This will shuffle all music known to the app.", R.drawable.ic_shuffle),
                    UIDialogs.Companion.GuideItem("Smart Shuffle", "This will shuffle your rated music, with higher ratings showing up earlier/more likely.\n\nThis is being improved.", R.drawable.ic_imagine),
                    UIDialogs.Companion.GuideItem("Home", "Here you find various subsections of items.\nMost are self explanatory.\nItems are ordered by last played, otherwise by item count.", R.drawable.ic_home)
                ), true)
            }

        }

        fun showNewPlaylistDialog() {
            UIDialogs.showCreatePlaylistDialog(context, fragment.lifecycleScope) {
                fragment.clearCache();
                updateContent();
            };
        }

        fun updateContent() {
            fragment.lifecycleScope.launch(Dispatchers.IO) {
                fragment._dataCacheTime?.let {
                    if(it < StateApp.instance.homeRefreshTime) {
                        fragment._dataRecent = null;
                        fragment._dataAlbums = null;
                        fragment._dataArtists = null;
                        fragment._dataSongNew = null;
                        fragment._dataPlaylists = null;
                        fragment._dataCacheTime = null;
                        fragment._dataUnrated = null;
                        fragment._dataMostPlayed = null;
                    }
                }

                var recent = fragment._dataRecent ?: StateDatabase.instance.getRecentPlays();
                var artists = fragment._dataArtists ?: StateDatabase.instance.getArtistsByRecent();
                var albums = fragment._dataAlbums ?:  StateDatabase.instance.getAlbumsByRecent();
                var playlists = (fragment._dataPlaylists ?: StateDatabase.instance.getPlaylistsByRecent()).map { it as IPlayable };
                val songNew = fragment._dataSongNew ?: StateDatabase.instance.getTracksNew(20);

                val unrated = fragment._dataUnrated ?: StateDatabase.instance.getTracksUnrated(20);
                val mostPlayed = fragment._dataMostPlayed ?: StateDatabase.instance.getMostPlayed(20);

                //val vibeWeighted = fragment._dataVibeWeighted ?: Vibe("Suggested", ImageVariable.fromResource(R.drawable.ic_playlist), listOf(), listOf(), StateDatabase.instance.getTrackListWeighted(100));

                //TODO: Move this out somwehere else?
                if(Settings.instance.developer.isShowcaseMode) {
                    UIDialogs.appToast("SHOWCASE MODE ENABLED, FILTERED RESULTS");
                    if(playlists.size == 0) {
                        val id1 = StateDatabase.instance.db.playlistDao().insert(DBPlaylist(name = "Movie Music")).first()
                        val id2 = StateDatabase.instance.db.playlistDao().insert(DBPlaylist(name = "Favorites")).first();
                        StateDatabase.instance.addTrackToPlaylist(id1, StateDatabase.instance.searchTracks("Border Reiver").first().id);
                        StateDatabase.instance.addTrackToPlaylist(id1, StateDatabase.instance.searchTracks("Goa").first().id);
                        StateDatabase.instance.addTrackToPlaylist(id1, StateDatabase.instance.searchTracks("Howard Shore / Concerning Hobbits").first().id);
                        StateDatabase.instance.addTrackToPlaylist(id1, StateDatabase.instance.searchTracks("In The House - In A Heartbeat").first().id);
                        StateDatabase.instance.addTrackToPlaylist(id1, StateDatabase.instance.searchTracks("Howard Shore / The Shadow Of").first().id);
                        StateDatabase.instance.addTrackToPlaylist(id1, StateDatabase.instance.searchTracks("Howard Shore / The Black Rider").first().id);
                        StateDatabase.instance.addTrackToPlaylist(id2, StateDatabase.instance.searchTracks("Chandelier").first().id);
                        StateDatabase.instance.addTrackToPlaylist(id2, StateDatabase.instance.searchTracks("Wild Child").first().id);
                        StateDatabase.instance.addTrackToPlaylist(id2, StateDatabase.instance.searchTracks("Heat Of The Moment").first().id);
                        StateDatabase.instance.addTrackToPlaylist(id2, StateDatabase.instance.searchTracks("Californication").first().id);
                        StateDatabase.instance.updatePlaylistMetadata(id1);
                        StateDatabase.instance.updatePlaylistMetadata(id2);
                        playlists = (fragment._dataPlaylists ?: StateDatabase.instance.getPlaylistsByRecent()).map { it as IPlayable };
                    }

                    val showcaseRecent = listOf("Asia", "Queen II", "The Dark Side Of The Moon", "Revolver")
                        .mapNotNull { albums.find { album -> album.name.trim() == it} };
                    recent = if(showcaseRecent.isEmpty())
                        recent.filter { val n = it.getImage(); return@filter !(n?.isEmpty ?: true) }
                    else showcaseRecent;
                    artists = artists.filter { val n = it.getImage(); return@filter !(n?.isEmpty ?: true) }
                    albums = albums.filter { val n = it.getImage(); return@filter !(n?.isEmpty ?: true) }
                }


                //fragment._dataRecent = recent;
                fragment._dataArtists = artists;
                fragment._dataAlbums = albums;
                fragment._dataSongNew = songNew;
                //fragment._dataVibeWeighted = vibeWeighted;
                fragment._dataCacheTime = OffsetDateTime.now();
                fragment._dataUnrated = unrated;
                fragment._dataMostPlayed = mostPlayed;

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
                        fragment._recentStateSave?.let {
                            gridRecent.recycler.post {
                                gridRecent.recycler.layoutManager?.onRestoreInstanceState(it);
                            }
                        }
                    }
                    if(playlists.isNullOrEmpty()) {
                        gridPlaylists.visibility = View.GONE;
                        containerPlaylistsCreate.visibility = View.VISIBLE;
                    }
                    else {
                        gridPlaylists.setData(playlists);
                        gridPlaylists.visibility = View.VISIBLE;
                        containerPlaylistsCreate.visibility = View.GONE;
                        fragment._playlistStateSave?.let {
                            gridPlaylists.recycler.post {
                                gridPlaylists.recycler.layoutManager?.onRestoreInstanceState(it);
                            }
                        }
                    }
                    if(artists.isNullOrEmpty())
                        gridArtists.visibility = View.GONE;
                    else {
                        gridArtists.setData(artists);
                        gridArtists.visibility = View.VISIBLE;
                        fragment._artistStateSave?.let {
                            gridArtists.recycler.post {
                                gridArtists.recycler.layoutManager?.onRestoreInstanceState(it);
                            }
                        }
                    }
                    if(albums.isNullOrEmpty())
                        gridAlbums.visibility = View.GONE;
                    else {
                        gridAlbums.setData(albums);
                        gridAlbums.visibility = View.VISIBLE;
                        fragment._albumStateSave?.let {
                            gridAlbums.recycler.post {
                                gridAlbums.recycler.layoutManager?.onRestoreInstanceState(it);
                            }
                        }
                    }

                    if(unrated.isNullOrEmpty())
                        gridUnrated.visibility = View.GONE;
                    else {
                        gridUnrated.setData(unrated);
                        gridUnrated.visibility = View.VISIBLE;
                        fragment._unratedStateSave?.let {
                            gridUnrated.recycler.post {
                                gridUnrated.recycler.layoutManager?.onRestoreInstanceState(it);
                            }
                        }
                    }
                    if(mostPlayed.isNullOrEmpty())
                        gridMostPlayed.visibility = View.GONE;
                    else {
                        gridMostPlayed.setData(mostPlayed);
                        gridMostPlayed.visibility = View.VISIBLE;
                        fragment._mostPlayedStateSave?.let {
                            gridMostPlayed.recycler.post {
                                gridMostPlayed.recycler.layoutManager?.onRestoreInstanceState(it);
                            }
                        }
                    }
                    if(songNew.isNullOrEmpty())
                        gridSongRecent.visibility = View.GONE;
                    else {
                        gridSongRecent.setData(songNew);
                        gridSongRecent.visibility = View.VISIBLE;
                        fragment._songNewStateSave?.let {
                            gridSongRecent.recycler.post {
                                gridSongRecent.recycler.layoutManager?.onRestoreInstanceState(it);
                            }
                        }
                    }
                    fragment._scrollY?.let {
                        scroller.post {
                            scroller.scrollTo(0, it);
                        }
                    }
                }
            }
            //val artists = StateLibrary.instance.getArtists(fragment.requireContext(), ArtistOrdering.TrackCount).map { it.toArtist() } as List<IPlayable>;
            //val albums = StateLibrary.instance.getAlbums(fragment.requireContext()).map { it.toAlbum() } as List<IPlayable>;

        }

        fun onShown(paramter: Any? = null) {
            updateContent();
            //StateApp.instance.activity()?.setBackgroundTopGradient(Color.rgb(10, 9, 39), 2f, 1f)
            StateApp.instance.activity()?.setBackgroundTop(resources.getDrawable(R.drawable.background_glow), 2.5f, 0.5f);
        }

        fun onHide() {
        }
    }
}