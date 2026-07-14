package com.futo.music.files

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import com.futo.music.states.StateApp

//Use this instead of DocumentFile, its slow AF.
class FastDocumentFile {

    val id: String;
    val uri: String;
    val name: String;
    val mimeType: String;

    val isDirectory: Boolean get() = mimeType == DocumentsContract.Document.MIME_TYPE_DIR;

    constructor(docId: String, path: String, name: String, mimeType: String) {
        this.id = docId;
        this.uri = path;
        this.name = name;
        this.mimeType = mimeType;
    }

    fun getFiles(): List<FastDocumentFile> {
        val resolver = StateApp.instance.activity()?.contentResolver ?: return listOf();

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(Uri.parse(uri), id);
        val result = ArrayList<FastDocumentFile>();

        val cursor = resolver.query(childrenUri, FAST_DOC_COLUMNS, null, null, null) ?: return result;

        cursor.use {
            while(cursor.moveToNext()) {
                val docId = cursor.getString(0);
                result.add(readFastDoc(DocumentsContract.buildDocumentUriUsingTree(Uri.parse(uri), docId).toString(), cursor));
            }
        }

        return result;
    }

    fun readAsText(context: Context): String? {
        return context.contentResolver.openInputStream(Uri.parse(uri))?.bufferedReader()?.use {
            it?.readText();
        };
    }


    companion object {
        val FAST_DOC_COLUMNS = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE);

        private fun readColumns(cursor: Cursor): FastDocumentFileColumns {
            return FastDocumentFileColumns(
                cursor.getString(0),
                cursor.getString(1),
                cursor.getString(2));
        }
        private fun readFastDoc(uri: String, cursor: Cursor): FastDocumentFile {
            return FastDocumentFile(
                cursor.getString(0),
                uri,
                cursor.getString(1),
                cursor.getString(2));
        }


        fun fromUri(context: Context, uri: Uri): FastDocumentFile? {
            val documentUri = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri));

            val cursor = context.contentResolver?.query(documentUri, FAST_DOC_COLUMNS, null, null, null) ?: return null;

            if(!cursor.moveToFirst()) {
                cursor.close();
                return null;
            }
            val doc = readFastDoc(uri.toString(), cursor);
            cursor.close();
            return doc;
        }
    }

    data class FastDocumentFileColumns(
        val id: String,
        val name: String,
        val mimeType: String
    )
}