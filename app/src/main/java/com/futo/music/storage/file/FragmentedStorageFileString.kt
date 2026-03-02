package com.futo.music.storage.file

open class FragmentedStorageFileString : FragmentedStorageFile() {
    var value : String? = null;

    override fun encode(): String {
        return value ?: "";
    }
    open fun decode(str: String) {
        value = str;
    }
}