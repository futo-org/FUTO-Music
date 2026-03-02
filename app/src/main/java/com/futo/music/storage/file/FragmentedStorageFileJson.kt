package com.futo.music.storage.file

import kotlinx.serialization.json.Json

abstract class FragmentedStorageFileJson : FragmentedStorageFile() {

    override fun encode(): String {
        return Json.encodeToString(this);
    }
}