package com.futo.music.images

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.futo.music.GradientType
import java.security.MessageDigest

class GradientFadeTransformation(val type: GradientType, val reverse: Boolean = false) : BitmapTransformation() {

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        val width = toTransform.width
        val height = toTransform.height
        val output = pool.get(width, height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        canvas.drawBitmap(toTransform, 0f, 0f, paint)

        val gradient =

            LinearGradient(0f, 0f, 0f, height.toFloat(),intArrayOf(Color.WHITE, Color.TRANSPARENT),null,Shader.TileMode.CLAMP)

        paint.shader = gradient
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        return output
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update("gradient_fade_v1".toByteArray())
    }
}