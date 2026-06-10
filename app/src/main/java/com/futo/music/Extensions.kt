package com.futo.music

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.core.view.isVisible
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.futo.music.fragments.main.AlbumFragment
import com.futo.music.fragments.main.ArtistFragment
import com.futo.music.fragments.main.MainFragment
import com.futo.music.fragments.main.PlaybackFragment
import com.futo.music.fragments.main.PlaylistFragment
import com.futo.music.models.playable.IPlayable
import com.futo.music.models.playable.IPlayableTrack
import com.futo.music.settings.Settings
import com.futo.music.states.StateApp
import com.futo.music.states.StateQueue
import com.futo.music.storage.db.DBAlbum
import com.futo.music.storage.db.DBArtist
import com.futo.music.storage.db.DBPlaylist
import com.futo.music.storage.db.DBTrack
import com.futo.music.ui.views.general.SortDropdown
import com.futo.music.ui.views.general.SortDropdownType
import jp.wasabeef.glide.transformations.BitmapTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val shuffle: Boolean = false,
    val index: Int = -1
)
fun IPlayable.withSettings(settings: PlaySettings): IPlayableWithPlaySettings {
    return IPlayableWithPlaySettings(this, settings);
}


fun IPlayable.openPlayable(fragment: MainFragment, preferMenu: Boolean = false, parentPlayable: IPlayable? = null) {
    if(this is DBArtist && !preferMenu)
        fragment.navigate<ArtistFragment>(this);
    else if(this is DBAlbum && !preferMenu)
        fragment.navigate<AlbumFragment>(this);
    else if(this is DBPlaylist && !preferMenu)
        fragment.navigate<PlaylistFragment>(this);
    else {
        if (preferMenu || (StateQueue.instance.getCurrentTrack() != null && Settings.instance.general.overlayWhenPlaying)) {
            UIDialogs.overlayPlayable(this, parentPlayable);
        } else {
            if(parentPlayable != null && Settings.instance.general.queueEntireCollection && this is IPlayableTrack) {
                StateApp.instance.scopeOrNull?.launch(Dispatchers.IO) {
                    val parentTracks = parentPlayable.getTracks(fragment.context ?: return@launch);
                    val index = parentTracks.indexOfFirst { it.getItemId() == this@openPlayable.getItemId() };
                    withContext(Dispatchers.Main) {
                        if (index >= 0)
                            fragment.navigate<PlaybackFragment>(
                                parentPlayable.withSettings(
                                    PlaySettings(
                                        index = index
                                    )
                                )
                            );
                        else
                            fragment.navigate<PlaybackFragment>(this);
                    }
                }
            }
            else
                fragment.navigate<PlaybackFragment>(this);
        }
    }
}

fun List<IPlayable>.sort(sortType: SortDropdownType?): List<IPlayable> {
    if(sortType == null)
        return this;
    return when(sortType){
        SortDropdownType.Alphabetic -> this.sortedBy { it.name.lowercase() };
        SortDropdownType.AlphabeticDesc -> this.sortedByDescending { it.name.lowercase() };
        SortDropdownType.Added -> this.sortedBy { it.getAddedDate() };
        SortDropdownType.AddedDesc -> this.sortedByDescending { it.getAddedDate() };
        SortDropdownType.Played -> this.sortedBy { it.getPlayedDate() };
        SortDropdownType.PlayedDesc -> this.sortedByDescending { it.getPlayedDate() };
        SortDropdownType.Count -> this.sortedBy { it.getItemCount() }
        SortDropdownType.CountDesc -> this.sortedByDescending { it.getItemCount() }
    }
}

fun IPlayable.getAddedDate(): OffsetDateTime {
    if(this is DBArtist)
        return this.dateAdded;
    if(this is DBAlbum)
        return this.dateAdded;
    if(this is DBPlaylist)
        return this.dateAdded;
    if(this is DBTrack)
        return this.dateAdded;
    return OffsetDateTime.MIN;
}
fun IPlayable.getPlayedDate(): OffsetDateTime {
    if(this is DBArtist)
        return this.datePlayed;
    if(this is DBAlbum)
        return this.datePlayed;
    if(this is DBPlaylist)
        return this.datePlayed;
    if(this is DBTrack)
        return this.datePlayed;
    return OffsetDateTime.MIN;
}
fun IPlayable.getItemCount(): Int {
    if(this is DBArtist)
        return this.trackCount;
    if(this is DBAlbum)
        return this.trackCount;
    if(this is DBPlaylist)
        return this.trackCount;
    if(this is DBTrack)
        return 1;
    return 1;
}

fun IPlayable.isHidden(): Boolean {
    if(this is DBArtist)
        return this.hidden;
    if(this is DBAlbum)
        return this.hidden;
    if(this is DBPlaylist)
        return this.hidden;
    if(this is DBTrack)
        return this.hidden;
    return false;
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
    return this.addListener(object: RequestListener<T> {
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
    return this.addListener(object: RequestListener<T> {
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
fun createLinearGradient(type: GradientType, reverse: Boolean, colors: IntArray, mode: Shader.TileMode): LinearGradient {
    val typeVal = type.value;
    if(reverse)
        colors.reverse();
    return LinearGradient(typeVal[0], typeVal[1], typeVal[2], typeVal[3], colors, null as FloatArray?, mode);
}

fun Int.toGradient(color: Int, gradientType: GradientType): LinearGradient {
    return LinearGradient(gradientType.value[0], gradientType.value[1], gradientType.value[2], gradientType.value[3], this, color, Shader.TileMode.MIRROR);
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

fun RecyclerView.setHeaderScrollFade(header: View, offset: Int, onVisibleChanged: ((Boolean)->Unit)? = null) {
    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            val layoutManager = layoutManager;
            if(layoutManager is LinearLayoutManager) {
                val firstVisible = layoutManager.findFirstVisibleItemPosition();
                if(firstVisible > 0) {
                    if(header.isVisible) {
                        header.isVisible = false;
                        onVisibleChanged?.invoke(false);
                    }
                }
                else {
                    if(!header.isVisible) {
                        header.isVisible = true;
                        onVisibleChanged?.invoke(true);
                    }

                    val sy = computeVerticalScrollOffset();
                    val max = header.measuredHeight - offset;
                    header.alpha = Math.max(0f, (1f - (sy.toFloat() / max)));
                }
            }
        }
    })
}

fun View.hideAnimated() {
    this.animate()
        .alpha(0f)
        .setDuration(300)
        .withEndAction {
            this.isVisible = false;
        }
        .start();
}
fun View.showAnimated() {
    this.alpha = 0f;
    this.isVisible = true;
    this.animate()
        .alpha(1f)
        .setDuration(300)
        .start();
}