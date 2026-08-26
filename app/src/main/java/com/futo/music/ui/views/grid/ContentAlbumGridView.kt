package com.futo.music.ui.views.grid

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.futo.music.R
import com.futo.music.constructs.Event1
import com.futo.music.dp
import com.futo.music.models.playable.Album
import com.futo.music.models.playable.IPlayable
import com.futo.music.models.playable.Vibe
import com.futo.music.storage.db.DBAlbum
import com.futo.music.storage.db.DBPlaylist
import com.futo.music.ui.views.AutoSizeLayout
import com.google.android.material.imageview.ShapeableImageView

class ContentAlbumGridView(viewGroup: ViewGroup, val asList: Boolean = false) : IContentGridView {

    override val root: ConstraintLayout;
    val imageThumbnail: ShapeableImageView;
    val textName: TextView;
    val textMeta: TextView;
    val textCount: TextView;

    var playableItem: IPlayable? = null;

    override val onClick = Event1<IPlayable>();
    override val onLongClick = Event1<IPlayable>();

    private var _hideMetadata: Boolean = false;

    init {
        root = LayoutInflater.from(viewGroup.context).inflate(if(!asList) R.layout.grid_album else R.layout.grid_track_list, viewGroup, false) as ConstraintLayout;
        imageThumbnail = root.findViewById(R.id.image_thumbnail);
        textName = root.findViewById(R.id.text_name);
        textMeta = root.findViewById(R.id.text_metadata);
        textCount = root.findViewById(R.id.text_count);

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
        val buttonOptions = root.findViewById<ImageButton?>(R.id.button_options);
        if(buttonOptions != null)
            buttonOptions.setOnClickListener {
                playableItem?.let {
                    onLongClick.emit(it);
                }
            }
    }

    override fun bind(playable: IPlayable) {
        playableItem = playable;
        textName.text = playable.name.trim();
        playable.getImage().let {
            val fallback = if(playable is DBAlbum)
                R.drawable.unknown_album
            else if(playable is DBPlaylist)
                R.drawable.unknown_playlist
            else
                R.drawable.unknown_music;
            if(it?.isEmpty == false)
                it.setImageView(imageThumbnail, fallback);
            else
                imageThumbnail.setImageResource(fallback);
        }
        if(playable is Album) {
            if(playable.artist == null) {
                textMeta.text = "Unknown Artist";
                textMeta.isVisible = true;
            }
            else {
                textMeta.text = playable.artist.name;
                textMeta.isVisible = true;
            }
        }
        else if(playable is DBAlbum) {
            if(playable.authors.isNotBlank()) {
                textMeta.text = playable.authors;
                textMeta.isVisible = true;
            }
            else {
                textMeta.text = "Unknown Artist";
                textMeta.isVisible = true;
            }
            if(_hideMetadata)
            {
                textMeta.isVisible = false
                textName.textAlignment = TextView.TEXT_ALIGNMENT_CENTER;
            }
            else {
                textName.textAlignment = TextView.TEXT_ALIGNMENT_TEXT_START
            }

            if(playable.trackDurations > 0) {
                textCount.text = playable.trackCount.toString();
                textCount.isVisible = true;
            }
            else {
                textCount.text = "";
                textCount.isVisible = false;
            }
        }
        else if(playable is Vibe) {
            textCount.text = playable.singles.size.toString();
            textMeta.isVisible = false;
        }
        else {
            textCount.isVisible = false;
            textMeta.isVisible = false;
        }
    }

    override fun setSize(width: Int, height: Int) {
        if(asList)
            return;

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
    }
}