package com.futo.music.ui.views.grid

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.futo.music.R
import com.futo.music.constructs.Event1
import com.futo.music.dp
import com.futo.music.models.playable.Album
import com.futo.music.models.playable.IPlayable
import com.futo.music.states.StateApp
import com.futo.music.storage.db.DBAlbum
import com.futo.music.storage.db.DBTrack
import com.futo.music.toHumanTime
import com.futo.music.toHumanTimeIndicator
import com.futo.music.ui.views.AutoSizeLayout
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContentTrackGridView(viewGroup: ViewGroup) : IContentGridView {

    override val root: ConstraintLayout;
    val imageThumbnail: ShapeableImageView;
    val textName: TextView;
    val textMeta: TextView;
    val textCount: TextView;
    val textTag: TextView;

    var playableItem: IPlayable? = null;

    override val onClick = Event1<IPlayable>();
    override val onLongClick = Event1<IPlayable>();

    private var _hideMetadata: Boolean = false;
    private var _showPlays: Boolean = false;

    init {
        root = LayoutInflater.from(viewGroup.context).inflate(R.layout.grid_track, viewGroup, false) as ConstraintLayout;
        imageThumbnail = root.findViewById(R.id.image_thumbnail);
        textName = root.findViewById(R.id.text_name);
        textMeta = root.findViewById(R.id.text_metadata);
        textCount = root.findViewById(R.id.text_count);
        textTag = root.findViewById(R.id.text_tag);

        root.setOnClickListener {
            playableItem?.let {
                onClick.emit(it);
            }
        }
        root.setOnLongClickListener {
            playableItem?.let {
                onLongClick.emit(it);
            }
            return@setOnLongClickListener true;
        }
    }

    override fun bind(playable: IPlayable) {
        playableItem = playable;
        textName.text = playable.name.trim();
        StateApp.instance.scopeOrNull?.launch(Dispatchers.IO) {
            playable.getImage().let {
                withContext(Dispatchers.Main) {
                    if(playableItem != playable) return@withContext
                    if (it?.isEmpty == false)
                        it.setImageView(imageThumbnail, R.drawable.unknown_music);
                    else
                        imageThumbnail.setImageResource(R.drawable.unknown_music);
                }
            }
        }
        if(playable is DBTrack) {
            if(playable.author.isNotBlank()) {
                textMeta.text = playable.author;
                textMeta.isVisible = true;
            }
            else {
                textMeta.text = "Unknown Artist";
                textMeta.isVisible = true;
            }
            if(playable.duration > 0) {
                textCount.text = playable.duration.toLong().toHumanTime(false)
                textCount.isVisible = true;
            }
            else {
                textCount.text = "";
                textCount.isVisible = false;
            }
        }

        if(_hideMetadata)
            textMeta.isVisible = false;

        if(_showPlays && playable is DBTrack) {
            textTag.text = if(playable.plays == 1) "1 play" else "${playable.plays} plays";
            textTag.isVisible = true;
        }
        else
            textTag.isVisible = false;

    }

    fun showPlays(show: Boolean) {
        _showPlays = show;
    }

    override fun setSize(width: Int, height: Int) {
        val dp10 = 10.dp(root.resources);
        val dp20 = 20.dp(root.resources);
        val dp40 = 40.dp(root.resources);
        root.updateLayoutParams {
            this.width = width - dp40;
        }
        imageThumbnail.updateLayoutParams {
            this.width = width - dp40 - dp10;
            this.height = width - dp40 - dp10;
        }
    }

    override fun setSettings(settings: GridSettings) {
        this._hideMetadata = settings.hideMetadata;
        this._showPlays = settings.showPlays;
    }
}