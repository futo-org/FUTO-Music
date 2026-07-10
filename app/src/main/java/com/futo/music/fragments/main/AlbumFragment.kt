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
import androidx.recyclerview.widget.RecyclerView
import com.futo.music.PlaySettings
import com.futo.music.R
import com.futo.music.UIDialogs
import com.futo.music.dp
import com.futo.music.fragments.MainFragView
import com.futo.music.fragments.top.NavigationTopBarFragment
import com.futo.music.models.playable.IPlayableTrack
import com.futo.music.openPlayable
import com.futo.music.setHeaderScrollFade
import com.futo.music.states.StateDatabase
import com.futo.music.storage.db.DBAlbum
import com.futo.music.storage.db.DBSetShuffleCombined
import com.futo.music.storage.db.DBTrack
import com.futo.music.ui.adapters.AnyInsertedAdapterView
import com.futo.music.ui.adapters.AnyInsertedAdapterView.Companion.asAnyWithViews
import com.futo.music.ui.viewholders.TrackAnyViewHolder
import com.futo.music.ui.views.NoResultsView
import com.futo.music.ui.views.containers.PlayableHeader
import com.futo.music.ui.views.containers.SettingsToggleView
import com.futo.music.withSettings
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlbumFragment: MainFragment() {
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


    class FragView(frag: AlbumFragment, inflater: LayoutInflater): MainFragView<AlbumFragment>(frag, inflater, R.layout.fragment_album) {

        val root: ConstraintLayout;

        val recycler: RecyclerView;

        val header: PlayableHeader;
        val emptyView: NoResultsView;

        var albumCurrent: DBAlbum? = null;

        var containerTop: ConstraintLayout;
        val buttonBack: ImageButton;
        val buttonOptions: ImageButton;

        private val _containerButtons: LinearLayout;
        //private val _buttonPlayAll: StandardButton;
        //private val _buttonShuffleAll: StandardButton;

        private val _adapter: AnyInsertedAdapterView<IPlayableTrack, TrackAnyViewHolder>;
        private var _adapterDataset: List<IPlayableTrack>? = null;


        init {
            root = findViewById(R.id.root);
            recycler = findViewById(R.id.recycler)
            header = PlayableHeader(context);
            emptyView = NoResultsView(context);

            containerTop = findViewById(R.id.container_top);
            buttonBack = findViewById(R.id.button_back);
            buttonOptions = findViewById(R.id.button_options);
            _containerButtons = findViewById(R.id.container_buttons);
            //_buttonPlayAll = findViewById(R.id.button_play_all);
            //_buttonShuffleAll = findViewById(R.id.button_shuffle_all)

            header.onPlayAll.subscribe {
                albumCurrent?.let {
                    fragment.navigate<PlaybackFragment>(it);
                }
            }
            header.onShuffleAll.subscribe {
                albumCurrent?.let {
                    fragment.navigate<PlaybackFragment>(it.withSettings(PlaySettings(shuffle = true)));
                }
            }

            _adapter = recycler.asAnyWithViews<IPlayableTrack, TrackAnyViewHolder>(arrayListOf<View>(header), arrayListOf<View>(emptyView), RecyclerView.VERTICAL, false, {
                it.useFullName = true;
                it.onClick.subscribe {
                    if(it is DBTrack)
                        it.openPlayable(fragment, parentPlayable = albumCurrent);
                }
                it.onLongClick.subscribe {
                    if(it is DBTrack)
                        it.openPlayable(fragment, true);
                }
            });

            val fadeOffset = 30.dp(resources);
            recycler.setHeaderScrollFade(containerTop, fadeOffset) {
                /*
                if(it)
                    _containerButtons.showAnimated();
                else
                    _containerButtons.hideAnimated();
                */
            }

            /*
            _buttonPlayAll.onClick.subscribe {
                fragment.navigate<PlaybackFragment>(albumCurrent ?: return@subscribe);
            }
            _buttonShuffleAll.onClick.subscribe {
                fragment.navigate<PlaybackFragment>(albumCurrent?.withSettings(PlaySettings(true)) ?: return@subscribe);
            }
            */

            header.onSearchChanged.subscribe { q -> //TODO: Implement efficient filtering on AnyAdapter
                if(q.isBlank())
                    _adapter.setData(_adapterDataset ?: return@subscribe);
                else
                    _adapter.setData(_adapterDataset?.filter { if(it is DBTrack) it.filter(q) else true } ?: return@subscribe);
            }
            buttonBack.setOnClickListener {
                fragment.closeSegment();
            }

            buttonOptions.setOnClickListener {
                fragment.lifecycleScope.launch(Dispatchers.IO) {
                    val currentAlbum = StateDatabase.instance.getAlbum(albumCurrent?.id ?: return@launch)

                    withContext(Dispatchers.Main) {
                        var dialog: BottomSheetDialog? = null;
                        dialog = UIDialogs.showSheet(context, LinearLayout(context).apply {
                            this.orientation = LinearLayout.VERTICAL;
                            this.addView(SettingsToggleView(context).apply {
                                this.setLabel("Shuffle Combined", "Play all songs in sequence if smart shuffle selects this track.");
                                this.setValue(currentAlbum?.shuffleCombined ?: false);
                                this.onValueChanged.subscribe { value ->
                                    currentAlbum?.let {
                                        fragment.lifecycleScope.launch(Dispatchers.IO) {
                                            StateDatabase.instance.db.albumDao().setShuffleCombined(DBSetShuffleCombined(it.id, value as Boolean));
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
            }
        }

        fun updateContent(albumId: Long) {
            findViewTreeLifecycleOwner()?.lifecycleScope?.launch(Dispatchers.IO) {
                val album = StateDatabase.instance.getAlbum(albumId) ?: return@launch;
                withContext(Dispatchers.Main) {
                    updateContent(album);
                }
            }
        }
        fun updateContent(album: DBAlbum) {
            albumCurrent = album;

            header.setPlayable(album, true);
            header.setMetadata("");
            header.clearSearch();
            fragment.topBar?.let {
                if(it is NavigationTopBarFragment) {
                    it.setTitle(album.name)
                }
            }

            fragment.lifecycleScope.launch(Dispatchers.IO) {

                val songs = StateDatabase.instance.getAlbumTracks(album.id);

                withContext(Dispatchers.Main) {
                    _adapter.setData(songs);
                    _adapterDataset = songs;

                    header.setMetadata("${songs.size} track" + (if(songs.size > 1) "s" else ""));

                    if (songs.isEmpty())
                        emptyView.isVisible = true;
                    else
                        emptyView.isVisible = false;
                }
            }
        }

        fun onShown(parameter: Any? = null) {
            if(parameter is DBAlbum)
                updateContent(parameter);
        }

        fun onHide() {

        }
    }
}