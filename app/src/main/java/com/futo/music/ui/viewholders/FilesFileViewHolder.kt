package com.futo.music.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.futo.music.R
import com.futo.music.constructs.Event2
import com.futo.music.files.DocumentFileItem
import com.futo.music.models.ImageVariable
import com.futo.music.states.StateApp
import com.futo.music.states.StateDatabase
import com.futo.music.storage.db.DBDirectory
import com.futo.music.storage.db.DBTrack
import com.futo.music.ui.adapters.IFileItem
import com.futo.music.ui.adapters.IFilesView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FilesFileViewHolder(val viewGroup: ViewGroup) : IFilesView {

    override val root: ConstraintLayout;
    private val _imageThumbnail: ImageView;
    private val _textName: TextView;
    private val _textMetadata: TextView;
    //private val _textCount: TextView;
    private val _button: ImageButton;

    override val onClick = Event2<IFilesView, IFileItem>();
    override val onOptions = Event2<IFilesView, IFileItem>();

    val onTrackChanged = Event2<IFilesView, IFileItem>();

    var item: IFileItem? = null;

    init {
        root = LayoutInflater.from(viewGroup.context).inflate(R.layout.list_directory_root, viewGroup, false) as ConstraintLayout;
        _imageThumbnail = root.findViewById(R.id.image_thumbnail);
        _textName = root.findViewById(R.id.text_name);
        _textMetadata = root.findViewById(R.id.text_metadata);
        _button = root.findViewById(R.id.button_more);
        root.setOnClickListener {
            item?.let {
                onClick.emit(this, it);
            }
        };
        _button.setOnClickListener {
            item?.let {
                onOptions.emit(this, it);
            }
        }
    }

    override fun bind(value: IFileItem) {
        if(value !is DocumentFileItem)
            return;

        _textName.text = value.name;
        _textMetadata.text = value.path;
        if(value.path.endsWith(".m3u"))
            _imageThumbnail.setImageResource(R.drawable.ic_link);
        else
            _imageThumbnail.setImageResource(R.drawable.ic_unknown_file);
        item = value;

        value.track.let {
            if(it != null)
                setDetailItem(it);
            else
                StateApp.instance.scopeOrNull?.launch(Dispatchers.IO) {
                    val track = StateDatabase.instance.getTrackByFileName(value.name);
                    //val image = track?.getImage();
                    if(track != null) {
                        value.track = track;
                        withContext(Dispatchers.Main) {
                            if (value == item) {
                                onTrackChanged?.emit(this@FilesFileViewHolder, value);
                                setDetailItem(track, null);
                            }
                        }
                    }
                }
        }
    }

    fun setDetailItem(track: DBTrack, image: ImageVariable? = null) {
        if(image != null && !image.isEmpty)
            image.setImageView(_imageThumbnail, R.drawable.unknown_music);
        else
            _imageThumbnail.setImageResource(R.drawable.unknown_music);
        _textName.text = track.name;
        _textMetadata.text = track.artistLine;
    }
}