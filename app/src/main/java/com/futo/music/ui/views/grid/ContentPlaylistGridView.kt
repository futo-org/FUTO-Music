package com.futo.music.ui.views.grid

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.bumptech.glide.Glide
import com.futo.music.R
import com.futo.music.constructs.Event1
import com.futo.music.dp
import com.futo.music.models.playable.IPlayable
import com.futo.music.storage.db.DBPlaylist
import com.futo.music.toHumanTimeIndicator
import com.google.android.material.imageview.ShapeableImageView

class ContentPlaylistGridView(viewGroup: ViewGroup) : IContentGridView {

    override val root: ConstraintLayout;
    val imageThumbnailShadow: ShapeableImageView;
    val imageThumbnail: ShapeableImageView;
    var imageThumbnail1: ImageView;
    val imageThumbnail2: ImageView;
    val imageThumbnail3: ImageView;
    val imageThumbnail4: ImageView;
    val textName: TextView;
    val textMeta: TextView;
    val textCount: TextView;

    val imageThumbnailGrid: List<ImageView>;

    var playableItem: IPlayable? = null;

    override val onClick = Event1<IPlayable>();

    init {
        root = LayoutInflater.from(viewGroup.context).inflate(R.layout.grid_playlist, viewGroup, false) as ConstraintLayout;
        imageThumbnail1 = root.findViewById(R.id.image_thumbnail);
        imageThumbnailShadow = root.findViewById(R.id.image_thumbnail_shadow);
        imageThumbnail = root.findViewById(R.id.image_thumbnail);
        textName = root.findViewById(R.id.text_name);
        textMeta = root.findViewById(R.id.text_metadata);
        textCount = root.findViewById(R.id.text_count);

        imageThumbnail1 = root.findViewById(R.id.image_thumbnail1);
        imageThumbnail2 = root.findViewById(R.id.image_thumbnail2);
        imageThumbnail3 = root.findViewById(R.id.image_thumbnail3);
        imageThumbnail4 = root.findViewById(R.id.image_thumbnail4);
        imageThumbnailGrid = listOf(
            imageThumbnail1,
            imageThumbnail2,
            imageThumbnail3,
            imageThumbnail4
        )

        root.setOnClickListener {
            playableItem?.let {
                onClick.emit(it);
            }
        }
    }

    override fun bind(playable: IPlayable) {
        playableItem = playable;
        textName.text = playable.name.trim();

        playable.getImage().let {
            if(it?.isEmpty == false)
                it.setImageView(imageThumbnail1, R.drawable.unknown_music);
            else
                imageThumbnail1.setImageResource(R.drawable.unknown_music);
        }
        if(playable is DBPlaylist) {
            val imgs = listOf(
                playable.artUri1,
                playable.artUri2,
                playable.artUri3,
                playable.artUri4
            ).filterNotNull().filter { it.isNotBlank() };

            imageThumbnailGrid.forEach { it.isVisible = true; };
            imageThumbnail.setImageResource(0);
            for(i in 0..3) {
                if(imgs.size > i) {
                    val thumb = imageThumbnailGrid[i];
                    val img = imgs[i];
                    Glide.with(thumb)
                        .load(img)
                        .fallback(R.drawable.unknown_music)
                        .into(thumb)
                }
                else
                    imageThumbnailGrid[i].setImageResource(R.drawable.unknown_music);
            }


            if(playable.trackDurations > 0) {
                textMeta.text = playable.trackDurations.toHumanTimeIndicator();
                textMeta.isVisible = true;
            }
            else {
                textMeta.text = "";
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
    }

    override fun setSize(width: Int, height: Int) {
        val dp10 = 10.dp(root.resources);
        val dp20 = 20.dp(root.resources);
        val dp40 = 40.dp(root.resources);
        root.updateLayoutParams {
            this.width = width - dp40;
        }
        val thumbWidth = width - dp40 - dp10;
        val thumbHeight = width - dp40 - dp10;
        imageThumbnail.updateLayoutParams {
            this.width = thumbWidth;
            this.height = thumbHeight;
        }
        imageThumbnailShadow.updateLayoutParams {
            this.width = thumbWidth
            this.height = thumbHeight
        }
        imageThumbnailGrid.forEach {
            it.updateLayoutParams {
                this.width = thumbWidth / 2;
                this.height = thumbHeight / 2;
            }
        }

    }
}