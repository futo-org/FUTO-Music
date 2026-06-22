package com.futo.music.logic.metadata

import com.futo.music.storage.db.MetadataType

interface IMetadataProvider {
    val type: MetadataType;

    fun guess(title: String): MetadataGuess;
}