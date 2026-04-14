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
import com.futo.music.models.playable.Vibe
import com.futo.music.storage.db.DBAlbum
import com.futo.music.ui.views.AutoSizeLayout
import com.google.android.material.imageview.ShapeableImageView

class ContentAlbumGridView(viewGroup: ViewGroup) : IContentGridView {

    override val root: ConstraintLayout;
    val imageThumbnail: ShapeableImageView;
    val textName: TextView;
    val textMeta: TextView;
    val textCount: TextView;

    var playableItem: IPlayable? = null;

    override val onClick = Event1<IPlayable>();
    override val onLongClick = Event1<IPlayable>();

    init {
        root = LayoutInflater.from(viewGroup.context).inflate(R.layout.grid_album, viewGroup, false) as ConstraintLayout;
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
    }

    override fun bind(playable: IPlayable) {
        playableItem = playable;
        textName.text = playable.name.trim();
        playable.getImage().let {
            if(it?.isEmpty == false)
                it.setImageView(imageThumbnail, R.drawable.unknown_music);
            else
                imageThumbnail.setImageResource(R.drawable.unknown_music);
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
}