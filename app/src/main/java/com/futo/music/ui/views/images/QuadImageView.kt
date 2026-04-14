package com.futo.music.ui.views.images

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.futo.music.R
import com.google.android.material.imageview.ShapeableImageView
import kotlin.collections.get

class QuadImageView: ConstraintLayout {

    val imageThumbnail: ShapeableImageView;
    var imageThumbnail1: ImageView;
    val imageThumbnail2: ImageView;
    val imageThumbnail3: ImageView;
    val imageThumbnail4: ImageView;

    val imageThumbnailGrid: List<ImageView>;

    constructor(context: Context, attrs: AttributeSet? = null) : super(context, attrs) {
        inflate(context, R.layout.image_quad, this);
        imageThumbnail1 = findViewById(R.id.image_thumbnail);
        imageThumbnail = findViewById(R.id.image_thumbnail);

        imageThumbnail1 = findViewById(R.id.image_thumbnail1);
        imageThumbnail2 = findViewById(R.id.image_thumbnail2);
        imageThumbnail3 = findViewById(R.id.image_thumbnail3);
        imageThumbnail4 = findViewById(R.id.image_thumbnail4);
        imageThumbnailGrid = listOf(
            imageThumbnail1,
            imageThumbnail2,
            imageThumbnail3,
            imageThumbnail4
        )

    }

    fun setImages(imgs: List<String>) {

        imageThumbnailGrid.forEach { it.isVisible = true; };
        imageThumbnail.setImageResource(0);
        for (i in 0..3) {
            if (imgs.size > i) {
                val thumb = imageThumbnailGrid[i];
                val img = imgs[i];
                Glide.with(thumb)
                    .load(img)
                    .fallback(R.drawable.unknown_music)
                    .into(thumb)
            } else
                imageThumbnailGrid[i].setImageResource(R.drawable.unknown_music);
        }
    }
}