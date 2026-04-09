package com.futo.music.storage.db

enum class MetadataType(val value: Int) {
    UNKNOWN(0),
    MEDIASTORE(1),
    INTERPRETED(2),
    EVALUATED(3);
}