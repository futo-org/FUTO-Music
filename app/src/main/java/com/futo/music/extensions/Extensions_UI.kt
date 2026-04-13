package com.futo.music.extensions

import android.graphics.Bitmap
import android.widget.ImageView
import androidx.media3.common.MediaMetadata
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.futo.music.PaletteColors
import com.futo.music.R
import com.futo.music.extractBitmap
import com.futo.music.extractColor
import com.futo.music.logging.Logger


fun ImageView.setAlbumArt(mediaMetadata: MediaMetadata, bitmapIntercept: ((Bitmap?)->Unit)? = null, colorIntercept: ((PaletteColors?)->Unit)? = null) {
    try {
        this.setImageResource(R.drawable.unknown_music);
        mediaMetadata.artworkDataType

        var builder = if (mediaMetadata.artworkUri != null)
            Glide.with(this)
                .load(mediaMetadata.artworkUri)
                .placeholder(R.drawable.unknown_music)
        else
            Glide.with(this)
                .load(mediaMetadata.artworkData)
                .placeholder(R.drawable.unknown_music)

        if(colorIntercept != null)
            builder = builder.extractColor(colorIntercept);

        if(bitmapIntercept != null)
            builder = builder.extractBitmap(bitmapIntercept);

        builder
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(this);
    }
    catch(ex: Throwable) {
        Logger.e("Glide", "Failed to set Album art", ex);
    }
}