package com.futo.music.models

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.ImageView
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.ViewTarget
import com.bumptech.glide.request.transition.Transition
import com.futo.music.R
import com.futo.music.logging.Logger
import kotlinx.serialization.Contextual
import kotlinx.serialization.Transient
import java.io.File

@kotlinx.serialization.Serializable
data class ImageVariable(
    val url: String? = null,
    val resId: Int? = null,
    @Transient
    @Contextual
    private val bitmap: Bitmap? = null
) {

    val isEmpty: Boolean get() {
        return url.isNullOrBlank() && (resId == null || resId == 0) && bitmap == null;
    }

    fun getGlideLoad(imageView: View): RequestBuilder<Drawable>? {
        if(bitmap != null) {
            return Glide.with(imageView)
                .load(bitmap)
        } else if(resId != null && resId > 0) {
            return Glide.with(imageView)
                .load(resId)
        } else if(!url.isNullOrEmpty()) {
            return Glide.with(imageView)
                .load(url)
        } else {
            return null;
        }
    }

    @SuppressLint("DiscouragedApi")
    fun setImageView(imageView: ImageView, fallbackResId: Int = -1) {
        if(bitmap != null) {
            Glide.with(imageView)
                .load(bitmap)
                .into(imageView)
        } else if(resId != null && resId > 0) {
            Glide.with(imageView)
                .load(resId)
                .into(imageView)
        } else if(!url.isNullOrEmpty()) {
            Glide.with(imageView)
                .load(url)
                .placeholder(fallbackResId)
                .fallback(fallbackResId)
                .into(imageView);
        } else if (fallbackResId != -1) {
            Glide.with(imageView)
                .load(fallbackResId)
                .into(imageView)
        } else {
            Glide.with(imageView)
                .clear(imageView)
        }
    }


    fun setViewGradientBackground(imageView: View) {
        val load = getGlideLoad(imageView);
        if(load != null) {
            load.apply(RequestOptions().override(2,2))
                .into(object: CustomViewTarget<View, Drawable>(imageView) {
                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                    val bitmap = resource.toBitmap(1, 2);
                    val topLeft = bitmap.getPixel(0, 0);
                    val botLeft = bitmap.getPixel(0, 1);
                    imageView.background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, arrayOf(topLeft, botLeft).toIntArray());
                }

                override fun onResourceCleared(placeholder: Drawable?) {
                    imageView.setBackgroundResource(0);
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    imageView.setBackgroundResource(0);
                }
            });
        }
        else {
            imageView.setBackgroundResource(0);
        }
    }

    companion object {
        fun fromUrl(url: String?): ImageVariable? {
            if(url == null)
                return null;
            return ImageVariable(url, null, null);
        }
        fun fromResource(id: Int): ImageVariable {
            return ImageVariable(null, id, null);
        }
        fun fromBitmap(bitmap: Bitmap): ImageVariable {
            return ImageVariable(null, null, bitmap);
        }
        fun fromFile(file: File): ImageVariable {
            try {
                return ImageVariable.fromBitmap(BitmapFactory.decodeFile(file.absolutePath));
            }
            catch(ex: Throwable) {
                Logger.e("ImageVariable", "Unsupported image format? " + ex.message, ex);
            return fromResource(0);
            //return fromResource(R.drawable.ic_error_pred);
            }
        }
    }
}