package com.futo.music.ui.views.playback

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.futo.music.R
import com.futo.music.constructs.Event0
import com.futo.music.extensions.setAlbumArt
import com.futo.music.logic.PlayerManager
import com.futo.music.states.StateDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaybackPeekView: ConstraintLayout {
    private val _root: ConstraintLayout;

    private val _imageArt: ImageView;
    private val _textTitle: TextView;
    private val _textArtist: TextView;
    private val _buttonPlay: ImageButton;
    private val _buttonNext: ImageButton;
    private val _buttonClose: ImageButton;

    private var _player: PlayerManager? = null;

    val onClick = Event0()


    constructor(context: Context, attrs: AttributeSet? = null): super(context, attrs) {
        inflate(context, R.layout.view_playback_peek, this);
        _root = findViewById<ConstraintLayout>(R.id.root);

        _imageArt = findViewById(R.id.image_art);
        _textTitle = findViewById(R.id.text_title);
        _textArtist = findViewById(R.id.text_artist);
        _buttonPlay = findViewById(R.id.button_play);
        _buttonNext = findViewById(R.id.button_next);
        _buttonClose = findViewById(R.id.button_close);

        _textTitle.isSelected = true;

        setOnClickListener {
            onClick.emit();
        }

        _buttonPlay.setOnClickListener {
            _player?.let {
                if(it.isPlaying)
                    it?.player?.pause();
                else
                    it?.player?.play();
            }
        }
        _buttonNext.setOnClickListener {
            _player?.player?.seekToNextMediaItem();
        }
        _buttonClose.setOnClickListener {
            _player?.player?.stop();
            this@PlaybackPeekView.visibility = GONE;
        }
    }

    fun setPlayer(player: PlayerManager) {
        _player = player;
        player.subscribeAndInvoke(this, object: PlayerManager.Listener {
            override fun onPlayingChanged(isPlaying: Boolean) {
                if(isPlaying)
                    _buttonPlay.setImageResource(R.drawable.ic_pause);
                else
                    _buttonPlay.setImageResource(R.drawable.ic_play);
            }
            override fun onMediaMetadataChanged(player: Player, mediaMetadata: MediaMetadata?) {
                if(mediaMetadata == null || mediaMetadata.title == null) {
                    if(player.currentMediaItem == null) {
                        this@PlaybackPeekView.visibility = GONE;
                        this@PlaybackPeekView._buttonNext.alpha =
                            if (player.hasNextMediaItem()) 1f else 0.3f;
                    }
                    return;

                }
                _imageArt.setAlbumArt(mediaMetadata);
                setTrackInfo(mediaMetadata?.title?.toString() ?: "", mediaMetadata?.artist?.toString() ?: "", player.hasNextMediaItem());
            }

            override fun onMediaItemChanged(player: Player, mediaItem: MediaItem?, reason: Int) {
                mediaItem?.mediaId?.toLongOrNull()?.let {
                    val id = it;
                    findViewTreeLifecycleOwner()?.lifecycleScope?.launch(Dispatchers.IO) {
                        val track = StateDatabase.instance.getTrack(id);
                        if(track != null) {
                            withContext(Dispatchers.Main) {
                                setTrackInfo(track.name, track.artistLine ?: "", player.hasNextMediaItem());
                            }
                        }
                    }
                }
            }

            fun setTrackInfo(name: String, artist: String?, hasNext: Boolean) {
                this@PlaybackPeekView._buttonNext.alpha =
                    if (hasNext) 1f else 0.3f;
                _textTitle.text = name;
                if(artist == null || artist.isBlank())
                    _textArtist.isVisible = false;
                else {
                    _textArtist.text = artist
                    _textArtist.isVisible = true;
                }
                this@PlaybackPeekView.visibility = VISIBLE;
            }

            override fun onMediaClose() {
                this@PlaybackPeekView.visibility = GONE;
            }
        })
    }
}