package com.futo.music.extensions

import android.widget.ImageView
import androidx.media3.common.MediaMetadata
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.futo.music.R


fun ImageView.setAlbumArt(mediaMetadata: MediaMetadata) {
    this.setImageResource(R.drawable.unknown_music);
    mediaMetadata.artworkDataType
    if(mediaMetadata.artworkUri != null)
        Glide.with(this)
            .load(mediaMetadata.artworkUri)
            .placeholder(R.drawable.unknown_music)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(this)
    else
        Glide.with(this)
            .load(mediaMetadata.artworkData)
            .placeholder(R.drawable.unknown_music)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(this)
}