package com.futo.music.ui.views.grid

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.futo.music.R
import com.futo.music.constructs.Event1
import com.futo.music.dp
import com.futo.music.models.playable.IPlayable
import com.futo.music.storage.db.DBArtist
import com.google.android.material.imageview.ShapeableImageView

class ContentArtistGridView(viewGroup: ViewGroup) : IContentGridView {

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
        root = LayoutInflater.from(viewGroup.context).inflate(R.layout.grid_artist, viewGroup, false) as ConstraintLayout;
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
        textName.text = playable.name;

        val image = playable.getImage();
        if(image != null)
            image.setImageView(imageThumbnail, R.drawable.ic_artist_thumbnail);
        else
            imageThumbnail.setImageResource(R.drawable.ic_artist_thumbnail);

        textMeta.isVisible = false;

        if(playable is DBArtist) {
            if(playable.trackDurations > 0) {
                textCount.text = playable.trackCount.toString();
                textCount.isVisible = true;
            }
            else {
                textCount.text = "";
                textCount.isVisible = false;
            }
        }
    }

    override fun setSize(width: Int, height: Int) {
        val dp10 = 10.dp(root.resources);
        val dp20 = 20.dp(root.resources);
        val dp40 = 40.dp(root.resources);
        root.updateLayoutParams {
            this.width = width - dp40;
        }
        val thumbnailSize = width - dp40 - dp10
        imageThumbnail.updateLayoutParams {
            this.width = width - dp40 - dp10;
            this.height = width - dp40 - dp10;
        }
        imageThumbnail.shapeAppearanceModel = imageThumbnail.shapeAppearanceModel.toBuilder()
            .setAllCornerSizes(thumbnailSize.toFloat() / 2)
            .build();
    }

    override fun setSettings(hideMetadata: Boolean) {
        this._hideMetadata = hideMetadata;
    }

    companion object {
    }
}