package com.futo.music.metadata

interface IMetadataProvider {
    fun determine(fullName: String): MetadataGuess?;
}