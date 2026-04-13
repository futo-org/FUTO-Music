package com.futo.music

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.palette.graphics.Palette
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.futo.music.fragments.main.ArtistFragment
import com.futo.music.fragments.main.MainFragment
import com.futo.music.fragments.main.PlaybackFragment
import com.futo.music.models.playable.IPlayable
import com.futo.music.states.StateApp
import com.futo.music.states.StateQueue
import com.futo.music.storage.db.DBArtist
import jp.wasabeef.glide.transformations.BitmapTransformation
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset


fun Long?.sToOffsetDateTimeUTC(): OffsetDateTime {
    if (this == null || this < 0)
        return OffsetDateTime.MIN
    if(this > 4070912400)
        return OffsetDateTime.MAX;
    return OffsetDateTime.ofInstant(Instant.ofEpochSecond(this), ZoneOffset.UTC)
}

fun Long?.msToOffsetDateTimeUTC(): OffsetDateTime {
    if (this == null || this < 0)
        return OffsetDateTime.MIN
    if(this > 4070912400)
        return OffsetDateTime.MAX;
    return OffsetDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneOffset.UTC)
}

fun String.levenshtein(b: String): Int {
    return levenshteinDistance(this, b)
};
fun levenshteinDistance(a: String, b: String): Int {
    if(a == b)
        return 0;
    if(a.length == 0)
        return b.length;
    if(b.length == 0)
        return a.length;

    val diffMat = Array(a.length + 1) { IntArray(b.length + 1) }

    for (i in 0..a.length)
        diffMat[i][0] = i
    for (j in 0..b.length)
        diffMat[0][j] = j

    for (i in 1..a.length) {
        for (j in 1..b.length) {
            val diff = if (a[i - 1] == b[j - 1]) 0 else 1
            diffMat[i][j] = minOf(diffMat[i - 1][j] + 1, diffMat[i][j - 1] + 1, diffMat[i - 1][j - 1] + diff)
        }
    }
    return diffMat[a.length][b.length]
}

class IPlayableWithPlaySettings(
    val playable: IPlayable,
    val playSettings: PlaySettings);

class PlaySettings(
    val shuffle: Boolean = false
)
fun IPlayable.withSettings(settings: PlaySettings): IPlayableWithPlaySettings {
    return IPlayableWithPlaySettings(this, settings);
}


fun IPlayable.openPlayable(fragment: MainFragment, preferMenu: Boolean = false) {
    if(this is DBArtist && !preferMenu)
        fragment.navigate<ArtistFragment>(this);
    else {
        if (preferMenu || StateQueue.instance.getCurrentTrack() != null) {
            UIDialogs.overlayPlayable(this);
        } else {
            fragment.navigate<PlaybackFragment>(this);
        }
    }
}



class GlidePaletteGenerator(val callback: (PaletteColors?)->Unit): BitmapTransformation() {
    override fun transform(context: Context, pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap? {

        Palette.from(toTransform)
            .generate {
                val colors = if(it != null) PaletteColors(it) else null;
                callback?.invoke(colors);
            }

        return toTransform;
    }


    override fun updateDiskCacheKey(messageDigest: MessageDigest) {

        //messageDigest.update("bitmap_interceptor_v1".toByteArray())
    }

    override fun equals(o: Any?): Boolean { //Not correct, but good enough for now
        return o is GlidePaletteGenerator && o.callback == callback;
    }

    override fun hashCode(): Int {
        return callback.hashCode();
    }
}

fun <T> RequestBuilder<T>.extractBitmap(handler: ((Bitmap?)->Unit)): RequestBuilder<T> {
    return this.listener(object: RequestListener<T> {
        override fun onLoadFailed(p0: GlideException?, p1: Any?, p2: Target<T?>?, p3: Boolean): Boolean {
            handler(null);
            return false;
        }

        override fun onResourceReady(p0: T?, p1: Any?, p2: Target<T?>?, p3: DataSource?, p4: Boolean): Boolean {
            if(p0 is Bitmap) {
                handler(p0);
            }
            else if(p0 is BitmapDrawable) {
                handler(p0.bitmap);
            }
            return false;
        }
    })
}
fun <T> RequestBuilder<T>.extractColor(handler: ((PaletteColors?)->Unit)): RequestBuilder<T> {
    return this.listener(object: RequestListener<T> {
        override fun onLoadFailed(p0: GlideException?, p1: Any?, p2: Target<T?>?, p3: Boolean): Boolean {
            handler(PaletteColors())
            return false;
        }

        override fun onResourceReady(p0: T?, p1: Any?, p2: Target<T?>?, p3: DataSource?, p4: Boolean): Boolean {
            if(p0 is Bitmap) {
                Palette.from(p0)
                    .generate {
                        if(it != null)
                            handler(PaletteColors(it));
                        else
                            handler(PaletteColors());
                    }
            }
            else if(p0 is BitmapDrawable) {
                Palette.from(p0.bitmap)
                    .generate {
                        if(it != null)
                            handler(PaletteColors(it));
                        else
                            handler(PaletteColors());
                    }
            }
            return false;
        }
    })
}

@Serializable
class PaletteColors {
    val swatches: Array<Int?>;

    val dominant: Int? get() = swatches[0];
    val darkVibrant: Int? get() = swatches[1];
    val vibrant: Int? get() = swatches[2];
    val muted: Int? get() = swatches[3];
    val darkMuted: Int? get() = swatches[4];
    val lightMuted: Int? get() = swatches[5];
    val lightVibrant: Int? get() = swatches[6];

    val hadPalette: Boolean;

    constructor() {
        swatches = arrayOf(null, null, null, null, null, null, null);
        hadPalette = false;
    }
    constructor(palette: Palette) {
        swatches = arrayOf(palette.dominantSwatch?.rgb, palette.darkVibrantSwatch?.rgb, palette.vibrantSwatch?.rgb, palette.mutedSwatch?.rgb, palette.darkMutedSwatch?.rgb, palette.lightMutedSwatch?.rgb, palette.lightVibrantSwatch?.rgb);
        hadPalette = true;
    }

    fun serialize(): String {
        return Json.encodeToString(this);
    }
    companion object{
        fun deserialize(str: String) {
            return Json.decodeFromString(str);
        }

    }


}

enum class GradientType(val value: Array<Float>) {
    Vertical(arrayOf(0.5f,0f, 0.5f, 1f)),
    Horizontal(arrayOf(0f,0.5f,1f,0.5f)),
    DiagonalLeft(arrayOf(0f,0f,1f,1f)),
    DiagonalRight(arrayOf(1f,0f,0f,1f))
}

fun Int.toGradient(color: Int, gradientType: GradientType): android.graphics.LinearGradient {
    return android.graphics.LinearGradient(gradientType.value[0], gradientType.value[1], gradientType.value[2], gradientType.value[3], this, color, Shader.TileMode.MIRROR);
}
fun Int.toGradientDrawable(color: Int, gradientType: GradientDrawable.Orientation): GradientDrawable {
    return GradientDrawable(gradientType, intArrayOf(this, color));
}

fun Int.colorIntensity(target: Int): Float {
    val red = Color.red(this);
    val green = Color.green(this);
    val blue = Color.blue(this);
    return (red + green + blue) / target.toFloat();
}