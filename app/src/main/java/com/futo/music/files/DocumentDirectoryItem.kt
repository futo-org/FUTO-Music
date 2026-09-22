package com.futo.music.files

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.futo.music.storage.db.DirectoryChildren
import com.futo.music.ui.adapters.FilesItemType
import com.futo.music.ui.adapters.IFileItem

class DocumentDirectoryItem: IFileItem {
    override val type = FilesItemType.Directory

    override val name: String;
    override val path: String;
    val uri: Uri;

    private var docFile: FastDocumentFile? = null;

    constructor(docFile: FastDocumentFile) {
        if(!docFile.isDirectory)
            throw IllegalArgumentException("Not a directory..: " + docFile.uri.toString());

        this.docFile = docFile;
        name = docFile.name ?: "NONAME";
        uri = docFile.uri;
        path = docFile.uri.toString()
    }

    fun getDirectoryChildren(): DirectoryChildren {
        val allFiles = docFile?.getFiles() ?: return DirectoryChildren("", listOf(),listOf());
        val dirs = allFiles.filter { it.isDirectory }.map { DocumentDirectoryItem(it) };
        val files = allFiles.filter { !it.isDirectory }.map { DocumentFileItem(it) };
        return DirectoryChildren(docFile!!.uri.toString(), dirs, files);
    }
    fun getFiles(): List<IFileItem> {
        val structure = getDirectoryChildren();
        return structure.directories + structure.files;
    }

    fun getDocFile(): FastDocumentFile?{
        return docFile;
    }


    companion object {
        fun fromPath(context: Context, path: String): DocumentDirectoryItem? {
            val doc = FastDocumentFile.fromUri(context, Uri.parse(path));
            return DocumentDirectoryItem(doc ?: return null);
        }
    }
}