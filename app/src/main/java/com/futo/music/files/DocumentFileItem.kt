package com.futo.music.files

import com.futo.music.storage.db.DBTrack
import com.futo.music.ui.adapters.FilesItemType
import com.futo.music.ui.adapters.IFileItem

class DocumentFileItem: IFileItem {
    override val type = FilesItemType.File
    override val name: String;
    override val path: String;

    val mimeType: String;

    var docFile: FastDocumentFile
        private set;

    var track: DBTrack? = null;


    constructor(docFile: FastDocumentFile) {
        if(docFile.isDirectory)
            throw IllegalArgumentException("Not a file..: " + docFile.uri);

        this.docFile = docFile;
        name = docFile.name ?: "NONAME";
        path = docFile.uri;
        mimeType = docFile.mimeType;
    }

    fun setDetailItem(track: DBTrack) {
        this.track = track;
    }

}