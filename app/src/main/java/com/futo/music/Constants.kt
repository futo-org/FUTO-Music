package com.futo.music

import android.content.Context

object Constants {

    val URL_APK = "https://music.grayjay.app/music/app-release.apk";
    val URL_APK_UNSTABLE = "https://music.grayjay.app/music/app-unstable-release.apk";
    val URL_VERSION = "https://music.grayjay.app/music/version.txt";
    val URL_VERSION_UNSTABLE = "https://music.grayjay.app/music/version-unstable.txt";
    val URL_CHANGELOG = "https://music.grayjay.app/music/changelogs/_VERSION_";
    val URL_CHANGELOG_UNSTABLE = "https://music.grayjay.app/music/changelogs-unstable/_VERSION_";
    val FILE_UPDATING = "update.apk";

    fun getApkUrl(): String = if(!BuildConfig.IS_UNSTABLE) URL_APK else URL_APK_UNSTABLE;
    fun getVersionUrl(): String = if(!BuildConfig.IS_UNSTABLE) URL_VERSION else URL_VERSION_UNSTABLE;
    fun getChangelogUrl(): String = if(!BuildConfig.IS_UNSTABLE) URL_CHANGELOG else URL_CHANGELOG_UNSTABLE;

}