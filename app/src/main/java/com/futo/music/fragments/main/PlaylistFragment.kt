package com.futo.music.fragments.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.futo.music.PlaySettings
import com.futo.music.R
import com.futo.music.UIDialogs
import com.futo.music.UIDialogs.ActionStyle
import com.futo.music.fragments.MainFragView
import com.futo.music.fragments.top.NavigationTopBarFragment
import com.futo.music.openPlayable
import com.futo.music.states.StateDatabase
import com.futo.music.storage.db.DBAlbum
import com.futo.music.storage.db.DBPlaylist
import com.futo.music.ui.buttons.RatingButton
import com.futo.music.ui.views.NoResultsView
import com.futo.music.ui.views.containers.ContentGrid
import com.futo.music.ui.views.general.SearchBarView
import com.futo.music.ui.views.images.QuadImageView
import com.futo.music.withSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaylistFragment: MainFragment() {
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


    class FragView(frag: PlaylistFragment, inflater: LayoutInflater): MainFragView<PlaylistFragment>(frag, inflater, R.layout.fragment_playlist) {

        val root: ConstraintLayout;

        val search: SearchBarView;

        val textName: TextView;
        val textMetadata: TextView;
        val imageHeader: QuadImageView;

        val gridSongs: ContentGrid;
        val emptyView: NoResultsView;

        val buttonPlayAll: LinearLayout;
        val buttonShuffle: LinearLayout;

        var playlistCurrent: DBPlaylist? = null;

        val buttonRating: RatingButton;
        val buttonDelete: ImageButton;


        init {
            root = findViewById(R.id.root);
            search = findViewById(R.id.view_search);
            gridSongs = findViewById(R.id.grid_songs);
            emptyView = findViewById(R.id.view_empty);

            textName = findViewById(R.id.text_name);
            textMetadata = findViewById(R.id.text_metadata);
            imageHeader = findViewById(R.id.image_header);

            buttonPlayAll = findViewById(R.id.button_play_all);
            buttonShuffle = findViewById(R.id.button_shuffle);

            buttonRating = findViewById(R.id.button_rating);
            buttonDelete = findViewById(R.id.button_delete);

            buttonDelete.setOnClickListener {
                val playlist = playlistCurrent ?: return@setOnClickListener;
                UIDialogs.showDialog(context, R.drawable.ic_playlist, playlist.name, "Are you sure you want to delete this playlist?", null, 0,
                    UIDialogs.Action("Cancel", {}, ActionStyle.NONE, true),
                    UIDialogs.Action("Delete", {
                        fragment.lifecycleScope.launch(Dispatchers.IO) {
                            StateDatabase.instance.deletePlaylist(playlist.id);
                            withContext(Dispatchers.Main) {
                                fragment.closeSegment();
                            }
                        }
                    }, ActionStyle.DANGEROUS))
            }

            gridSongs.onClick.subscribe {
                it.openPlayable(fragment);
            }
            gridSongs.onLongClick.subscribe {
                it.openPlayable(fragment, true);
            }

            buttonPlayAll.setOnClickListener {
                playlistCurrent?.let {
                    fragment.navigate<PlaybackFragment>(it);
                }
            }
            buttonShuffle.setOnClickListener {
                playlistCurrent?.let {
                    fragment.navigate<PlaybackFragment>(it.withSettings(PlaySettings(shuffle = true)));
                }
            }

            findViewById<ImageButton>(R.id.button_back).setOnClickListener {
                fragment.closeSegment();
            }

            search.onChange.subscribe {
                if(it.isEmpty())
                    gridSongs.clearSearch();
                else
                    gridSongs.search(it);
            }

            buttonRating.onClick.subscribe {
                playlistCurrent?.let {
                    UIDialogs.showRatingDialog(context, fragment.lifecycleScope, null, it, null, null, {
                       buttonRating.setRating(it.score);
                    });
                }
            }
        }

        fun updateContent(playlistId: Long) {
            findViewTreeLifecycleOwner()?.lifecycleScope?.launch(Dispatchers.IO) {
                val playlist = StateDatabase.instance.getPlaylist(playlistId) ?: return@launch;
                withContext(Dispatchers.Main) {
                    updateContent(playlist);
                }
            }
        }
        fun updateContent(playlist: DBPlaylist) {
            playlistCurrent = playlist;

            textName.text = playlist.name;
            fragment.topBar?.let {
                if(it is NavigationTopBarFragment) {
                    it.setTitle(playlist.name)
                }
            }
            buttonRating.setRating(playlist.score);


            val imgs = listOf(
                playlist.artUri1,
                playlist.artUri2,
                playlist.artUri3,
                playlist.artUri4
            ).filterNotNull().filter { it.isNotBlank() };
            imageHeader.setImages(imgs);

            fragment.lifecycleScope.launch(Dispatchers.IO) {

                val songs = StateDatabase.instance.getPlaylistTracks(playlist.id);

                withContext(Dispatchers.Main) {
                    gridSongs.setData(songs);

                    textMetadata.text = "${songs.size} song(s)";

                    if (songs.isEmpty())
                        gridSongs.isVisible = false;
                    else
                        gridSongs.isVisible = true;

                    if (songs.isEmpty())
                        emptyView.isVisible = true;
                    else
                        emptyView.isVisible = false;
                }
            }
        }

        fun onShown(parameter: Any? = null) {
            if(parameter is DBPlaylist)
                updateContent(parameter);

            search.clear();
        }

        fun onHide() {

        }
    }
}