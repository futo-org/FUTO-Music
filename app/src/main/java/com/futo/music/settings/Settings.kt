package com.futo.music.settings

import com.futo.music.R
import com.futo.music.UIDialogs
import com.futo.music.logging.Logger
import com.futo.music.states.StateApp
import com.futo.music.storage.file.FragmentedStorage
import com.futo.music.storage.file.FragmentedStorageFileJson
import com.futo.music.ui.views.containers.Setting
import com.futo.music.ui.views.containers.SettingsGroup
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter
import kotlin.streams.asStream
import kotlin.streams.toList


@Serializable
data class MenuBottomBarSetting(val id: Int, var enabled: Boolean);

@Serializable()
class Settings : FragmentedStorageFileJson() {
    var didFirstStart: Boolean = false;


    @Serializable
    class GeneralSettings {

        @Setting("Queue entire collections", "When tapping specific track in collection, queue entire collection instead of just one track")
        public var queueEntireCollection = true;

    }
    @SettingsGroup("General")
    var general = GeneralSettings();



    @Serializable
    class MediaSettings {

        @Setting("Scan for new media", "Checks the Android Mediastore for new media, this is automatically done on startup too.", icon = "ic_database_search", order = 0)
        fun scanForNewMedia() {
            val act = StateApp.instance.activity() ?: return;
            if(act.isSyncing)
                UIDialogs.appToast("Already scanning...");
            else {
                UIDialogs.appToast("Scan started, you can track progress in the app notifications");
                StateApp.instance.activity()?.sync(true);
            }
        }


        @Setting("Load Track Art", "Load embedded art in individual tracks, this is significantly heavier than just album art.", order = 1)
        public var loadTrackArt = true;

    }
    @SettingsGroup("Media")
    var media = MediaSettings();


    //region BOILERPLATE
    override fun encode(): String {
        return Json.encodeToString(this);
    }

    fun getGroups(): List<Pair<Any, SettingsGroup>> {
        return this::class.declaredMemberProperties
            .filter { it.findAnnotation<SettingsGroup>() != null && it.javaField != null }
            .map { Pair(it.javaGetter!!.invoke(this)!!, it.findAnnotation<SettingsGroup>()!!) }
            .toList();
    }

    companion object {
        private const val TAG = "Settings";

        private var _isFirst = true;

        val instance: Settings get() {
            if(_isFirst) {
                Logger.i(TAG, "Initial Settings fetch");
                _isFirst = false;
            }
            return FragmentedStorage.get<Settings>();
        }

        fun replace(text: String) {
            FragmentedStorage.replace<Settings>(text, true);
        }


        private fun preferedQualityToPixels(q: Int): Int {
            when (q) {
                0 -> return 1280 * 720;
                1 -> return 3840 * 2160;
                2 -> return 2560 * 1440;
                3 -> return 1920 * 1080;
                4 -> return 1280 * 720;
                5 -> return 854 * 480;
                6 -> return 640 * 360;
                7 -> return 426 * 240;
                8 -> return 256 * 144;
                else -> return 0;
            }
        }


        private fun threadIndexToCount(index: Int): Int {
            return when(index) {
                0 -> 1;
                1 -> 2;
                2 -> 4;
                3 -> 6;
                4 -> 8;
                5 -> 10;
                6 -> 15;
                else -> 1
            }
        }
    }
    //endregion
}
