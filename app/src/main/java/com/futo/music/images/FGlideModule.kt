package com.futo.music.images

import android.content.Context
import android.os.Build
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import java.io.InputStream
import java.nio.ByteBuffer

@GlideModule
class FGlideModule : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        Log.i("FAppGlideModule", "registerComponents called")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            registry.prepend(String::class.java, InputStream::class.java, MediaStoreThumbnailLoader.InputStreamFactory())
        }
    }
}