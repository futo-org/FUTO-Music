package com.futo.music.settings

import com.futo.music.logging.Logger
import com.futo.music.storage.file.FragmentedStorage
import com.futo.music.storage.file.FragmentedStorageFileJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


@Serializable
data class MenuBottomBarSetting(val id: Int, var enabled: Boolean);

@Serializable()
class Settings : FragmentedStorageFileJson() {
    var didFirstStart: Boolean = false;


    //region BOILERPLATE
    override fun encode(): String {
        return Json.encodeToString(this);
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
