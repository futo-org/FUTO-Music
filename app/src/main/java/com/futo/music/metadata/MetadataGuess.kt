package com.futo.music.metadata

import com.futo.music.storage.db.MetadataType

class MetadataGuess(
    val title: String,
    val artist: String,
    val certainty: Float,
    val type: MetadataType,
    val version: Int
)