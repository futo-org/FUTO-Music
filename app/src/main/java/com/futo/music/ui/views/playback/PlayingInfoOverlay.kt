package com.futo.music.ui.views.playback

import android.content.Context
import android.util.AttributeSet
import android.view.View.inflate
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.futo.music.R
import com.futo.music.UIDialogs
import com.futo.music.constructs.Event0
import com.futo.music.constructs.Event1
import com.futo.music.dp
import com.futo.music.fragments.main.PlaylistFragment
import com.futo.music.models.playable.IPlayable
import com.futo.music.models.playable.IPlayableTrack
import com.futo.music.states.StateApp
import com.futo.music.states.StateDatabase
import com.futo.music.states.StateQueue
import com.futo.music.storage.db.DBTrack
import com.futo.music.ui.buttons.RatingsButton
import com.futo.music.ui.viewholders.ListPlaylistViewHolder
import com.futo.music.ui.views.lists.ListPlaylistRating
import com.futo.music.ui.views.lists.TrackListEditorView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayingInfoOverlay: ConstraintLayout {

    val _root: ConstraintLayout;
    val _textTitle: TextView;

    val _containerArtist: ConstraintLayout;
    val _imageArtist: ImageView;
    val _textArtist: TextView;
    val _ratingArtist: RatingsButton;


    val _containerAlbum: ConstraintLayout;
    val _imageAlbum: ImageView;
    val _textAlbum: TextView;
    val _ratingAlbum: RatingsButton;


    val _containerPlaylists: ConstraintLayout;
    val _textPlaylists: TextView;
    val _listPlaylists: LinearLayout;


    val _buttonClose: ImageButton;


    var track: DBTrack? = null;

    val onClose = Event0();

    constructor(context: Context, attrs: AttributeSet? = null): super(context, attrs) {
        inflate(context, R.layout.view_playing_overlay, this);
        _root = findViewById(R.id.root);

        _containerArtist = findViewById(R.id.container_artist);
        _textArtist = findViewById(R.id.text_artist);
        _imageArtist = findViewById(R.id.image_artist);
        _ratingArtist = findViewById(R.id.rating_artist);
        _containerAlbum = findViewById(R.id.container_album);
        _textAlbum = findViewById(R.id.text_album);
        _imageAlbum = findViewById(R.id.image_album);
        _ratingAlbum = findViewById(R.id.rating_album);
        _containerPlaylists = findViewById(R.id.container_playlists);
        _listPlaylists = findViewById(R.id.list_playlists);
        _textPlaylists = findViewById(R.id.text_playlists);

        _textTitle = findViewById(R.id.text_title);
        _buttonClose = findViewById(R.id.button_close);

        _buttonClose.setOnClickListener {
            onClose.emit();
        }

    }

    fun setPlayable(item: DBTrack) {
        track = item;

        _textTitle.text = item.name;

        _containerArtist.isVisible = false;
        _containerAlbum.isVisible = false;
        _containerPlaylists.isVisible = false;

        StateApp.instance?.scopeOrNull?.launch(Dispatchers.IO) {
            val artists = StateDatabase.instance.getTrackArtists(item.id);

            if(!artists.isEmpty())
                withContext(Dispatchers.Main) {
                    _textArtist.text = artists[0].name;
                    artists[0].getImage()?.setImageView(_imageArtist);
                    _ratingArtist.setRatingsFor(artists[0]);
                    _containerArtist.isVisible = true;
                }

            val albums = StateDatabase.instance.getTrackAlbums(item.id);
            if(!albums.isEmpty())
                withContext(Dispatchers.Main) {
                    _textAlbum.text = albums[0].name;
                    albums[0].getImage()?.setImageView(_imageAlbum);
                    _ratingAlbum.setRatingsFor(albums[0]);
                    _containerAlbum.isVisible = true;
                }

            val dp5 = 5.dp(resources);

            val playlists = StateDatabase.instance.getTrackPlaylists(item.id);
            if(!playlists.isEmpty())
                withContext(Dispatchers.Main) {
                    val views = playlists.map { ListPlaylistRating(context).apply {
                        this.bind(it)
                        this.onClick.subscribe {
                            onClose.emit();
                            StateApp.instance.activity()?.navigate<PlaylistFragment>(it);
                        }
                        this.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                            this.setMargins(0, 0, 0, dp5);
                        }
                    } };
                    _listPlaylists.removeAllViews();
                    for(view in views)
                        _listPlaylists.addView(view);
                    _textPlaylists.text = if(playlists.size == 1) "1 Playlist" else "${playlists.size} Playlists";
                    _containerPlaylists.isVisible = true;
                }
        }


    }
}