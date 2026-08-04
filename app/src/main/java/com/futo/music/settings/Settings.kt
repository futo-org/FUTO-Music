package com.futo.music.settings

import androidx.lifecycle.lifecycleScope
import com.futo.music.BuildConfig
import com.futo.music.R
import com.futo.music.UIDialogs
import com.futo.music.fragments.main.BuyFragment
import com.futo.music.fragments.main.ContentsFragment
import com.futo.music.fragments.main.SearchFragment
import com.futo.music.logging.Logger
import com.futo.music.states.StateApp
import com.futo.music.states.StateDatabase
import com.futo.music.states.StatePayment
import com.futo.music.storage.db.DBAlbumUpdateRating
import com.futo.music.storage.db.DBArtistUpdateRating
import com.futo.music.storage.db.DBPlaylistUpdateRating
import com.futo.music.storage.db.DBTrackUpdateRating
import com.futo.music.storage.file.FragmentedStorage
import com.futo.music.storage.file.FragmentedStorageFileJson
import com.futo.music.storage.file.StringStorage
import com.futo.music.ui.views.containers.Setting
import com.futo.music.ui.views.containers.SettingType
import com.futo.music.ui.views.containers.SettingsGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
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
        @Setting("Buy FUTO Music", "Support the development of this app.", icon = "ic_money", order = 0, filterPlaystore = true)
        fun buyNav() {
            val act = StateApp.instance.activity() ?: return;
            act.navigate<BuyFragment>();
        }
        @Setting("Enter License Key", "If you already own a license key, click here.", icon = "ic_license", order = 1, filterPlaystore = true)
        fun enterLicense() {
            val act = StateApp.instance.activity() ?: return;
            UIDialogs.showInputDialog(act, act.lifecycleScope, "License Key", "Enter your license key from the email", "ex. ABCD-EF12-GH34-IJ56-...", "Activate") {
                if (it.isNullOrEmpty())
                    return@showInputDialog;

                val loader = UIDialogs.showLoader(act, "Checking your License..", "Give us a moment to validate your key!");
                act.lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val success = StatePayment.instance.setPaymentLicenseKey(it);
                        withContext(Dispatchers.Main) {
                            loader.hide();
                            if (success) {
                                UIDialogs.appToast("Your app has been activated!");
                                act.closeSegment(act.fragCurrent);
                            } else
                                UIDialogs.appToast("Invalid license key");
                        }
                    }
                    catch(ex: Throwable) {
                        withContext(Dispatchers.Main) {
                            loader.hide();
                            //TODO: Payment backend atm always returns exception if invalid, fix that, then change this flow.
                            //UIDialogs.appToast("Error: " + ex.message);
                            UIDialogs.appToast("Invalid license key");
                        }
                    }
                }
            };
        }
        @Setting("Activation Status", "If this app is activated", order = 2, type = SettingType.INFO)
        public val activated: String get() =
            if(!StatePayment.instance.isTesting)
                (if(StatePayment.instance.hasPaid) "Activated" else if(BuildConfig.IS_PLAYSTORE_BUILD) "Playstore" else "Not Activated")
            else
                (if(StatePayment.instance.hasPaid) "Activated (Test)" else if(BuildConfig.IS_PLAYSTORE_BUILD) "Playstore (Test)" else "Not Activated (Test)")

        //@Setting("Queue Entire Collections", "When tapping specific track in collection, queue entire collection instead of just one track", order = 1)
        public var queueEntireCollection = true;

        //@Setting("Options When Queue Exists", "When you already have a queue or are playing media, when tapping a song it will show options instead of playing that song directly to avoid clearing your queue.", order = 2)
        public var overlayWhenPlaying = true;

    }
    @SettingsGroup("General", 0)
    var general = GeneralSettings();




    @Serializable
    class MediaSettings {

        @Setting("Scan For New Media", "Checks the Android Mediastore for new media, this is automatically done on startup too.", icon = "ic_database_search", order = 0)
        fun scanForNewMedia() {
            val act = StateApp.instance.activity() ?: return;
            if(act.isSyncing)
                UIDialogs.appToast("Already scanning...");
            else {
                UIDialogs.appToast("Scan started, you can track progress in the app notifications");
                StateApp.instance.activity()?.sync(true);
            }
        }

        @Setting("Show Hidden Items", "Allows you to see media you have hidden, this then allows you to unhide these items.", icon = "ic_hide", order = 1)
        fun showHiddenMedia() {
            val act = StateApp.instance.activity() ?: return;

            StateApp.instance.scopeOrNull?.launch(Dispatchers.IO) {
                val hiddenItems = StateDatabase.instance.db.albumDao().getAllHidden() +
                            StateDatabase.instance.db.artistDao().getAllHidden() +
                            StateDatabase.instance.db.playlistDao().getAllHidden() +
                            StateDatabase.instance.db.tracksDao().getAllHidden()
                withContext(Dispatchers.Main) {
                    act.navigate<ContentsFragment>(Triple("Hidden", hiddenItems, listOf(1)));
                    act.setRefresher {
                        showHiddenMedia();
                    }
                }
            }
        }

        @Setting("Load Track Art", "Load embedded art in individual tracks, this is significantly heavier than just album art.", order = 9)
        public var loadTrackArt = true;

    }
    @SettingsGroup("Media", 2)
    var media = MediaSettings();


    @Serializable
    class AboutSettings {

        @Setting("Version", "The build version", order = 1, type = SettingType.INFO)
        public val version: Int get() = BuildConfig.VERSION_CODE;
        @Setting("Version Type", "The build type", order = 2, type = SettingType.INFO)
        public val versionType: String get() = BuildConfig.FLAVOR;


        /*
        @Setting("Test Throw", "")
        fun TestException(){
            throw NotImplementedError("Test");
        }*/
    }
    @SettingsGroup("About", 999)
    var about = AboutSettings();

    @Serializable
    class DeveloperSettings {

        @Setting("Developer", "Are you a developer?", order = 1, type = SettingType.TOGGLE)
        public var isDeveloper: Boolean = false;


        @Setting("Reset Ratings", "Resets all ratings to defaults")
        fun ResetRatings(){
            UIDialogs.showConfirmDialog(StateApp.instance.activity() ?: return, R.drawable.ic_gear, "Reset Ratings?", "This cannot be undone, are you sure?", {
                StateApp.instance.scopeOrNull?.launch(Dispatchers.IO) {
                    UIDialogs.appToast("Fetching Ids (0/5)..");
                    val idTracks = StateDatabase.instance.db.tracksDao().getAllIds();
                    val idAlbums = StateDatabase.instance.db.albumDao().getAllIds();
                    val idArtists = StateDatabase.instance.db.artistDao().getAllIds();
                    val idPlaylists = StateDatabase.instance.db.playlistDao().getAllIds();

                    UIDialogs.appToast("Starting clearing (1/5)..");

                    for(id in idTracks)
                        StateDatabase.instance.db.tracksDao().setRating(DBTrackUpdateRating(id, 0));
                    UIDialogs.appToast("Reset track ratings (2/5)..");
                    for(id in idAlbums)
                        StateDatabase.instance.db.albumDao().setRating(DBAlbumUpdateRating(id, 0));
                    UIDialogs.appToast("Reset album ratings (3/5)..");
                    for(id in idArtists)
                        StateDatabase.instance.db.artistDao().setRating(DBArtistUpdateRating(id, 0));
                    UIDialogs.appToast("Reset artist ratings (4/5)..");
                    for(id in idPlaylists)
                        StateDatabase.instance.db.playlistDao().setRating(DBPlaylistUpdateRating(id, 0));
                    UIDialogs.appToast("Reset playlist ratings (5/5)..");

                    UIDialogs.appToast("All ratings reset, may need to reload views.");
                }
            });
        }

        @Setting("Clear First Startup", "Reset initial startup, does not clear database")
        fun clearStartup() {
            FragmentedStorage.get<StringStorage>("showedAlpha").setAndSave("");
            UIDialogs.toast("Cleared startup boolean");
        }

        //@Setting("ShowCase Mode", "HIDES ITEMS WITHOUT THUMBNAIL ON HOME DO NOT TURN ON", order = 2, type = SettingType.TOGGLE)
        @Transient
        public val isShowcaseMode: Boolean = false;

    }
    @SettingsGroup("Developer", 9999)
    var developer = DeveloperSettings();

    //region BOILERPLATE
    override fun encode(): String {
        return Json.encodeToString(this);
    }

    fun getGroups(): List<Pair<Any, SettingsGroup>> {
        return this::class.declaredMemberProperties
            .filter { it.findAnnotation<SettingsGroup>() != null && it.javaField != null }
            .map { Pair(it.javaGetter!!.invoke(this)!!, it.findAnnotation<SettingsGroup>()!!) }
            .sortedBy { it.second.order }
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
