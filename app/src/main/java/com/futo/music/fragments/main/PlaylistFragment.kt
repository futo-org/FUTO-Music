package com.futo.music.fragments.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.futo.music.PlaySettings
import com.futo.music.R
import com.futo.music.UIDialogs
import com.futo.music.dp
import com.futo.music.fragments.MainFragView
import com.futo.music.fragments.top.NavigationTopBarFragment
import com.futo.music.models.playable.IPlayableTrack
import com.futo.music.openPlayable
import com.futo.music.setHeaderScrollFade
import com.futo.music.states.StateApp
import com.futo.music.states.StateDatabase
import com.futo.music.storage.db.DBPlaylist
import com.futo.music.storage.db.DBSetShuffleCombined
import com.futo.music.storage.db.DBTrack
import com.futo.music.ui.buttons.ListButton
import com.futo.music.ui.views.NoResultsView
import com.futo.music.ui.views.containers.PlayableHeader
import com.futo.music.ui.views.containers.SettingsToggleView
import com.futo.music.ui.views.lists.TrackListEditorView
import com.futo.music.withSettings
import com.google.android.material.bottomsheet.BottomSheetDialog
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

    class FragView(frag: PlaylistFragment, inflater: LayoutInflater): MainFragView<PlaylistFragment>(frag, inflater, R.layout.fragment_playlist) {

        val root: ConstraintLayout
        //val recycler: RecyclerView
        val header: PlayableHeader
        val emptyView: NoResultsView

        var playlistCurrent: DBPlaylist? = null

        var containerTop: ConstraintLayout
        val buttonBack: ImageButton
        val buttonOptions: ImageButton;

        val trackList: TrackListEditorView;

        private val _containerButtons: LinearLayout
        //private val _adapter: AnyInsertedAdapterView<IPlayableTrack, TrackAnyViewHolder>
        private var _adapterDataset: List<IPlayableTrack>? = null

        init {
            root = findViewById(R.id.root)
            header = findViewById<PlayableHeader>(R.id.header)//PlayableHeader(context)
            emptyView = NoResultsView(context)
            trackList = findViewById(R.id.track_editor)

            containerTop = findViewById(R.id.container_top)
            buttonBack = findViewById(R.id.button_back)
            buttonOptions = findViewById(R.id.button_options);
            _containerButtons = findViewById(R.id.container_buttons)

            trackList.setThumbnailsVisible(false);

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

            /*
            _adapter = recycler.asAnyWithViews<IPlayableTrack, TrackAnyViewHolder>(arrayListOf(), arrayListOf(emptyView), RecyclerView.VERTICAL, false) {
                it.useFullName = true
                it.onClick.subscribe {
                    if (it is DBTrack)
                        it.openPlayable(fragment)
                }
                it.onLongClick.subscribe {
                    if (it is DBTrack)
                        it.openPlayable(fragment, true)
                }
            }*/

            val fadeOffset = 30.dp(resources)

            trackList.recycler.setHeaderScrollFade(containerTop, fadeOffset) {
                // no-op for now
            }
            trackList.onTrackClicked.subscribe {
                if (it is DBTrack)
                    it.openPlayable(fragment, parentPlayable = playlistCurrent)
            }
            trackList.onTrackOptions.subscribe {
                if (it is DBTrack)
                    it.openPlayable(fragment, true)
            }
            trackList.onTrackRemoved.subscribe { track ->
                if(track is DBTrack)
                    fragment.lifecycleScope.launch(Dispatchers.IO) {
                        StateDatabase.instance.removeTrackFromPlaylist(playlistCurrent?.id ?: return@launch, track.id);
                        StateDatabase.instance.updatePlaylistMetadata(playlistCurrent?.id ?:return@launch);
                        StateApp.instance?.activity()?.getFragment<HomeFragment>()?.clearPlaylistsCache();
                    }
            }
            trackList.onTrackOrderChanged.subscribe {
                val tracks = it.filterIsInstance<DBTrack>();
                if(tracks.size > 0)
                    fragment.lifecycleScope.launch(Dispatchers.IO) {
                        StateDatabase.instance.reorderPlaylist(
                            playlistCurrent?.id ?: return@launch, tracks
                        );
                    }
            }

            header.onSearchChanged.subscribe { q ->
                if (q.isBlank()) {
                    trackList.setEditable(true);
                    trackList.setFilter("");
                }
                else {
                    trackList.setEditable(false);
                    trackList.setFilter(q);
                }
            }
            buttonBack.setOnClickListener {
                fragment.closeSegment()
            }
            buttonOptions.setOnClickListener {
                var dialog: BottomSheetDialog? = null;
                dialog = UIDialogs.showSheet(context, LinearLayout(context).apply {
                    this.orientation = LinearLayout.VERTICAL;
                    this.addView(ListButton(context).withData(R.drawable.ic_trash, "Delete") {
                        dialog!!.hide();
                        UIDialogs.showConfirmSheet(context, R.drawable.ic_trash, "Delete [${playlistCurrent?.name}]", "Are you sure you want to delete [${playlistCurrent?.name}]?", {
                            fragment.lifecycleScope.launch(Dispatchers.IO) {
                                playlistCurrent?.let {
                                    StateDatabase.instance.deletePlaylist(it.id);
                                }
                                withContext(Dispatchers.Main) {
                                    fragment.closeSegment();
                                }
                            }
                        });
                    });
                    this.addView(SettingsToggleView(context).apply {
                        this.setLabel("Shuffle Combined", "Play all songs in sequence if smart shuffle selects this track.");
                        this.setValue(playlistCurrent?.shuffleCombined ?: false);
                        this.onValueChanged.subscribe { value ->
                            playlistCurrent?.let {
                                fragment.lifecycleScope.launch(Dispatchers.IO) {
                                    StateDatabase.instance.db.playlistDao().setShuffleCombined(DBSetShuffleCombined(it.id, value as Boolean));
                                }
                            }
                        }
                        this.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                            val dp5 = 5.dp(resources);
                            this.setMargins(dp5, dp5, dp5, dp5);
                        }
                    })
                }, {

                }, true)
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
                val songs = StateDatabase.instance.getPlaylistTracks(playlist.id);

                withContext(Dispatchers.Main) {
                    //_adapter.setData(songs)
                    trackList.setTracks(ArrayList(songs), header.search.getText().isBlank());
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
