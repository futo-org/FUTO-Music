package com.futo.music.fragments.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
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
import com.futo.music.colorIntensity
import com.futo.music.extractColor
import com.futo.music.fragments.MainFragView
import com.futo.music.fragments.top.NavigationTopBarFragment
import com.futo.music.openPlayable
import com.futo.music.states.StateApp
import com.futo.music.states.StateDatabase
import com.futo.music.storage.db.DBAlbum
import com.futo.music.storage.db.DBArtist
import com.futo.music.ui.buttons.RatingButton
import com.futo.music.ui.views.NoResultsView
import com.futo.music.ui.views.containers.ContentGrid
import com.futo.music.ui.views.general.SearchBarView
import com.futo.music.withSettings
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

        val search: SearchBarView;

        val textName: TextView;
        val textMetadata: TextView;
        val imageHeader: ImageView;

        val gridSongs: ContentGrid;
        val emptyView: NoResultsView;

        val buttonPlayAll: LinearLayout;
        val buttonShuffle: LinearLayout;

        var albumCurrent: DBAlbum? = null;

        val buttonRating: RatingButton;


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

            gridSongs.onClick.subscribe {
                it.openPlayable(fragment);
            }
            gridSongs.onLongClick.subscribe {
                it.openPlayable(fragment, true);
            }

            buttonPlayAll.setOnClickListener {
                albumCurrent?.let {
                    fragment.navigate<PlaybackFragment>(it);
                }
            }
            buttonShuffle.setOnClickListener {
                albumCurrent?.let {
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
                albumCurrent?.let {
                    UIDialogs.showRatingDialog(context, fragment.lifecycleScope, null, null, it, null, {
                       buttonRating.setRating(it.score);
                    });
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

            textName.text = album.name;
            fragment.topBar?.let {
                if(it is NavigationTopBarFragment) {
                    it.setTitle(album.name)
                }
            }
            buttonRating.setRating(album.score);
            Glide.with(imageHeader)
                .load(album.artUri)
                .fallback(R.drawable.background_button_black)
                .extractColor { pal ->
                    StateApp.instance.activity()?.let {
                        if(pal != null && (pal.dominant ?: pal.darkVibrant) != null) {
                            val color = (pal.dominant ?: pal.darkVibrant!!);
                            val intensity = 1f / color.colorIntensity(180);
                            it.setBackgroundBottomGradient(color, 0.5f, Math.min(1f, intensity));
                        }
                        else
                            it.hideBackgroundBottom();
                    }
                }
                .into(imageHeader);

            fragment.lifecycleScope.launch(Dispatchers.IO) {

                val songs = StateDatabase.instance.getAlbumTracks(album.id);

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
            if(parameter is DBAlbum)
                updateContent(parameter);
        }

        fun onHide() {

        }
    }
}