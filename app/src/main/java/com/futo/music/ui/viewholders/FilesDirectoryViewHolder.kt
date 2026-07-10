package com.futo.music.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.futo.music.R
import com.futo.music.constructs.Event2
import com.futo.music.files.DocumentDirectoryItem
import com.futo.music.storage.db.DBDirectory
import com.futo.music.ui.adapters.IFileItem
import com.futo.music.ui.adapters.IFilesView

class FilesDirectoryViewHolder(val viewGroup: ViewGroup) : IFilesView {

    override val root: ConstraintLayout;
    private val _imageThumbnail: ImageView;
    private val _textName: TextView;
    private val _textMetadata: TextView;
    //private val _textCount: TextView;
    private val _button: ImageButton;

    override val onClick = Event2<IFilesView, IFileItem>();
    override val onOptions = Event2<IFilesView, IFileItem>();
    var item: IFileItem? = null;

    init {
        root = LayoutInflater.from(viewGroup.context).inflate(R.layout.list_directory_root, viewGroup, false) as ConstraintLayout;
        _imageThumbnail = root.findViewById(R.id.image_thumbnail);
        _textName = root.findViewById(R.id.text_name);
        _textMetadata = root.findViewById(R.id.text_metadata);
        _button = root.findViewById(R.id.button_more);
        _button.isVisible = false;
        root.setOnClickListener {
            item?.let {
                onClick.emit(this, it);
            }
        }
        _button.setOnClickListener {
            item?.let {
                onOptions.emit(this, it);
            }
        }
    }

    override fun bind(value: IFileItem) {
        if(value !is DocumentDirectoryItem)
            return;
        _textName.text = value.name;
        _textMetadata.text = value.path;

        item = value;
    }

}