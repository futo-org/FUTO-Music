package com.futo.music.fragments.main

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.collection.emptyLongSet
import androidx.compose.animation.core.updateTransition
import androidx.core.view.isVisible
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.futo.music.PlaySettings
import com.futo.music.R
import com.futo.music.UIDialogs
import com.futo.music.fragments.MainFragView
import com.futo.music.fragments.top.NavigationTopBarFragment
import com.futo.music.models.playable.IPlayable
import com.futo.music.states.ArtistOrdering
import com.futo.music.states.StateDatabase
import com.futo.music.states.StateLibrary
import com.futo.music.storage.db.DBArtist
import com.futo.music.ui.views.NoResultsView
import com.futo.music.ui.views.containers.ContentGrid
import com.futo.music.ui.views.general.SearchBarView
import com.futo.music.withSettings
import jp.wasabeef.glide.transformations.BlurTransformation
import jp.wasabeef.glide.transformations.MaskTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ArtistFragment: MainFragment() {
    override val isMainView : Boolean = true;
    override val isTab: Boolean = true;
    override val hasBottomBar: Boolean get() = false;

    override val fragmentTitle: String = "Artist";

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

        val search: SearchBarView;

        val textName: TextView;
        val textMetadata: TextView;
        val imageHeader: ImageView;

        val gridSongs: ContentGrid;
        val gridAlbums: ContentGrid;
        val emptyView: NoResultsView;

        val buttonPlayAll: LinearLayout;
        val buttonShuffle: LinearLayout;

        var artistCurrent: DBArtist? = null;


        init {
            search = findViewById(R.id.view_search);
            gridAlbums = findViewById(R.id.grid_albums);
            gridSongs = findViewById(R.id.grid_songs);
            emptyView = findViewById(R.id.view_empty);

            textName = findViewById(R.id.text_name);
            textMetadata = findViewById(R.id.text_metadata);
            imageHeader = findViewById(R.id.image_header);

            buttonPlayAll = findViewById(R.id.button_play_all)
            buttonShuffle = findViewById(R.id.button_shuffle)

            gridAlbums.onClick.subscribe {
                UIDialogs.overlayPlayable(it);
            }
            gridSongs.onClick.subscribe {
                UIDialogs.overlayPlayable(it);
            }

            buttonPlayAll.setOnClickListener {
                artistCurrent?.let {
                    fragment.navigate<PlaybackFragment>(it);
                }
            }
            buttonShuffle.setOnClickListener {
                artistCurrent?.let {
                    fragment.navigate<PlaybackFragment>(it.withSettings(PlaySettings(shuffle = true)));
                }
            }

            findViewById<ImageButton>(R.id.button_back).setOnClickListener {
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

            textName.text = artist.name;
            fragment.topBar?.let {
                if(it is NavigationTopBarFragment) {
                    it.setTitle(artist.name)
                }
            }
            Glide.with(imageHeader)
                .load(artist.artUri)
                .fallback(R.drawable.background_button_black)
                .into(imageHeader);

            fragment.lifecycleScope.launch(Dispatchers.IO) {

                val albums = StateDatabase.instance.db.albumDao().getArtistAlbums(artist.id).sortedByDescending { it.datePlayed }
                val songs = StateDatabase.instance.getArtistTracks(artist.id).sortedByDescending { it.plays };

                withContext(Dispatchers.Main) {
                    gridAlbums.setData(albums);
                    gridSongs.setData(songs);

                    textMetadata.text = "${albums.size} album(s), ${songs.size} song(s)";

                    if (albums.isEmpty())
                        gridAlbums.isVisible = false;
                    else
                        gridAlbums.isVisible = false;

                    if (songs.isEmpty())
                        gridSongs.isVisible = false;
                    else
                        gridSongs.isVisible = true;

                    if (albums.isEmpty() && songs.isEmpty())
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