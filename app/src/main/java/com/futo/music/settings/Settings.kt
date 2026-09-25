package com.futo.music.settings

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.lifecycleScope
import com.futo.music.BuildConfig
import com.futo.music.R
import com.futo.music.R.array.shuffle_reoccurrence
import com.futo.music.R.array.shuffle_reoccurrence_time
import com.futo.music.R.array.shuffle_algorithm
import com.futo.music.UIDialogs
import com.futo.music.UIDialogs.ActionStyle
import com.futo.music.fragments.main.BuyFragment
import com.futo.music.fragments.main.ContentsFragment
import com.futo.music.logging.Logger
import com.futo.music.logic.shuffles.ESmartShuffle
import com.futo.music.logic.shuffles.ESmartShuffleV2
import com.futo.music.logic.shuffles.ScoreContainer
import com.futo.music.logic.shuffles.SmartShuffle
import com.futo.music.states.StateApp
import com.futo.music.states.StateDatabase
import com.futo.music.states.StateFiles
import com.futo.music.states.StatePayment
import com.futo.music.storage.db.DBAlbumUpdateRating
import com.futo.music.storage.db.DBArtistUpdateRating
import com.futo.music.storage.db.DBPlaylistUpdateRating
import com.futo.music.storage.db.DBTrackUpdateRating
import com.futo.music.storage.db.DBTrackUpdateRatingDone
import com.futo.music.storage.file.FragmentedStorage
import com.futo.music.storage.file.FragmentedStorageFileJson
import com.futo.music.storage.file.StringStorage
import com.futo.music.ui.dialogs.ProgressDialog
import com.futo.music.ui.views.containers.Setting
import com.futo.music.ui.views.containers.SettingDropdownOptions
import com.futo.music.ui.views.containers.SettingType
import com.futo.music.ui.views.containers.SettingsGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import java.time.OffsetDateTime
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter


@Serializable
data class MenuBottomBarSetting(val id: Int, var enabled: Boolean);

@Serializable()
class Settings : FragmentedStorageFileJson() {
    var didFirstStart: Boolean = false;


    @Serializable
    class GeneralSettings {

        @Setting("Activation Status", "If this app is activated", order = 0, type = SettingType.INFO)
        public val activated: String get() =
            if(!StatePayment.instance.isTesting)
                (if(StatePayment.instance.hasPaid) "Activated" else if(BuildConfig.IS_PLAYSTORE_BUILD) "Playstore" else "Not Activated")
            else
                (if(StatePayment.instance.hasPaid) "Activated (Test)" else if(BuildConfig.IS_PLAYSTORE_BUILD) "Playstore (Test)" else "Not Activated (Test)")

        @Setting("Buy FUTO Music", "Support the development of this app.", icon = "ic_money", order = 1, filterPlaystore = true)
        fun buyNav() {
            val act = StateApp.instance.activity() ?: return;
            act.navigate<BuyFragment>();
        }
        @Setting("Enter License Key", "If you already own a license key, click here.", icon = "ic_license", order = 2, filterPlaystore = true)
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

        @Setting("Report Issue on Github", "Open our Github repository to submit an issue", icon = "ic_bug", order = 3, filterPlaystore = false)
        fun openGithub() {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/futo-org/futo-music"))
            StateApp.instance.activity()?.startActivity(intent)
        }

        @Setting("Prefer List View", "Default to list views instead of grid views where possible", order = 9)
        public var preferListView = false;


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
                StateApp.instance.activity()?.sync(true, false) {
                    StateFiles.instance.scanAndProcessAll(act);
                }
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

        @Setting("Filter By Directories", "Filter your Home by showing only the tracks that are part of directories you add to the app", order = 10)
        public var filterByFiles = false;



        @Setting("Export Ratings", "Creates an export file containing your ratings, either by filename or MediaStore ID", icon = "ic_stars", order = 15)
        fun exportRatings() {
            val exportName = "export-${OffsetDateTime.now().toEpochSecond()}.json";
                StateApp.instance.activity()?.let { act ->
                UIDialogs.showDialog(act, R.drawable.ic_stars, false, "Export Ratings", "How would you like to export?\nBy (File)Name is for different devices.\nBy MSID is for same device.", null, null, null, 0,
                    UIDialogs.Action("Cancel", {}),
                    UIDialogs.Action("By Name", {
                        act.lifecycleScope.launch(Dispatchers.IO) {
                            val scoresTracks = StateDatabase.instance.db.tracksDao().getTrackScoresByMSID().distinctBy { it.mediaStoreId }.associate { Pair(it.mediaStoreId, it.score) };
                            val scoresAlbums = StateDatabase.instance.db.albumDao().getAlbumScoresByMSID().distinctBy { it.mediaStoreId }.associate { Pair(it.mediaStoreId, it.score) };
                            val scoresArtists = StateDatabase.instance.db.artistDao().getArtistScoresByMSID().distinctBy { it.mediaStoreId }.associate { Pair(it.mediaStoreId, it.score) };
                            val scoresPlaylists = StateDatabase.instance.db.playlistDao().getPlaylistsScoresByName().distinctBy { it.name }.associate { Pair(it.name, it.score) };

                            val export = ExportRatings(
                                tracksByMSID = scoresTracks,
                                albumsByMSID = scoresAlbums,
                                artistsByMSID = scoresArtists,
                                playlistsByName = scoresPlaylists
                            );
                            val json = Json.encodeToString(export);

                            StateApp.instance.saveFileJson(exportName, json);
                            //StateApp.instance.shareData("Export Name-Score", "application/json", exportName, json);
                        }
                    }, ActionStyle.PRIMARY),
                    UIDialogs.Action("By MSID", {
                        act.lifecycleScope.launch(Dispatchers.IO) {
                            val scoresTracks = StateDatabase.instance.db.tracksDao().getTrackScoresByName().distinctBy { it.fileName }.associate { Pair(it.fileName, it.score) };
                            val scoresAlbums = StateDatabase.instance.db.albumDao().getAlbumScoresByName().distinctBy { it.name }.associate { Pair(it.name, it.score) };
                            val scoresArtists = StateDatabase.instance.db.artistDao().getArtistScoresByName().distinctBy { it.name }.associate { Pair(it.name, it.score) };
                            val scoresPlaylists = StateDatabase.instance.db.playlistDao().getPlaylistsScoresByName().distinctBy { it.name }.associate { Pair(it.name, it.score) };

                            val export = ExportRatings(
                                tracksByName = scoresTracks,
                                albumsByName = scoresAlbums,
                                artistsByName = scoresArtists,
                                playlistsByName = scoresPlaylists
                            );
                            val json = Json.encodeToString(export);

                            StateApp.instance.saveFileJson(exportName, json);
                            //StateApp.instance.shareData("Export MSID-Score", "application/json", exportName, json);
                        }
                    }, ActionStyle.PRIMARY))
            }
        }
        @Setting("Import Ratings", "Imports a rating json export", icon = "ic_stars", order = 16)
        fun importRatings() {
            StateApp.instance.activity()?.let { act ->
                StateApp.instance.pickFile({
                    var dialog: ProgressDialog? = null;
                    act.lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val text = act.contentResolver
                                .openInputStream(it ?: return@launch)
                                ?.bufferedReader()
                                ?.use { it.readText() }
                                ?: "";
                            if (text.isNullOrEmpty()) {
                                UIDialogs.appToast("File empty or unable to read");
                            } else {
                                val forceShowProgress = true;
                                val export = Json.decodeFromString<ExportRatings>(text);
                                withContext(Dispatchers.Main) {
                                    dialog = UIDialogs.showDialogProgress(act, {

                                    });
                                    dialog.setTitle("Importing");
                                    dialog.setText("Importing ${export.totalCount()} ratings");
                                }
                                val total = export.totalCount().toDouble();
                                var fin = 0;
                                for (trackMSID in export.tracksByMSID ?: mapOf()) {
                                    val trackIds = StateDatabase.instance.getTrackIdsByMSID(trackMSID.key) ?: continue;
                                    StateDatabase.instance.setRatingTrack(trackIds.id, trackMSID.value);
                                    fin++;
                                    if (fin % 10 == 0 || forceShowProgress)
                                        withContext(Dispatchers.Main) { dialog?.setProgress(fin / total); }
                                }
                                for (trackName in export.tracksByName ?: mapOf()) {
                                    val trackId = StateDatabase.instance.getTrackIdByFileName(trackName.key) ?: continue;
                                    StateDatabase.instance.setRatingTrack(trackId, trackName.value);
                                    fin++;
                                    if (fin % 10 == 0 || forceShowProgress)
                                        withContext(Dispatchers.Main) {dialog?.setProgress(fin / total); }
                                }
                                for (albumMSID in export.albumsByMSID ?: mapOf()) {
                                    val album = StateDatabase.instance.getAlbumByMSID(albumMSID.key) ?: continue;
                                    StateDatabase.instance.setRatingAlbum(album.id, albumMSID.value);
                                    fin++;
                                    if (fin % 10 == 0 || forceShowProgress)
                                        withContext(Dispatchers.Main) { dialog?.setProgress(fin / total); }
                                }
                                for (albumName in export.albumsByName ?: mapOf()) {
                                    val album = StateDatabase.instance.getAlbumByName(albumName.key) ?: continue;
                                    StateDatabase.instance.setRatingAlbum(album.id, albumName.value);
                                    fin++;
                                    if (fin % 10 == 0 || forceShowProgress)
                                        withContext(Dispatchers.Main) { dialog?.setProgress(fin / total); }
                                }
                                for (artistMSID in export.artistsByMSID ?: mapOf()) {
                                    val artist = StateDatabase.instance.getAlbumByMSID(artistMSID.key) ?: continue;
                                    StateDatabase.instance.setRatingArtist(artist.id, artistMSID.value);
                                    fin++;
                                    if (fin % 10 == 0 || forceShowProgress)
                                        withContext(Dispatchers.Main) { dialog?.setProgress(fin / total); }
                                }
                                for (artistName in export.artistsByName ?: mapOf()) {
                                    val artist = StateDatabase.instance.getArtistByName(artistName.key) ?: continue;
                                    StateDatabase.instance.setRatingArtist(artist.id, artistName.value);
                                    fin++;
                                    if (fin % 10 == 0 || forceShowProgress)
                                        withContext(Dispatchers.Main) { dialog?.setProgress(fin / total); }
                                }
                                for (playlistName in export.playlistsByName ?: mapOf()) {
                                    val playlist = StateDatabase.instance.getPlaylistByName(playlistName.key) ?: continue;
                                    StateDatabase.instance.setRatingPlaylist(playlist.id, playlistName.value);
                                    fin++;
                                    if (fin % 10 == 0 || forceShowProgress)
                                        withContext(Dispatchers.Main) { dialog?.setProgress(fin / total); }
                                }
                                dialog?.setProgress(1.0);
                                UIDialogs.appToast("Imported ${fin} ratings");
                            }
                        } catch (ex: Throwable) {
                            Logger.e(TAG, "Failed to import file\n" + ex.message, ex);
                            UIDialogs.appToast("Failed to import file\n" + ex.message);
                        } finally {
                            withContext(Dispatchers.Main) { dialog?.hide(); }
                        }
                    }
                }, arrayOf("*/*"));
            }
        }

        @Serializable
        data class ExportRatings(
            val tracksByMSID: Map<Long, Int>? = null,
            val tracksByName: Map<String, Int>? = null,
            val albumsByMSID: Map<Long, Int>? = null,
            val albumsByName: Map<String, Int>? = null,
            val artistsByMSID: Map<Long, Int>? = null,
            val artistsByName: Map<String, Int>? = null,
            val playlistsByName: Map<String, Int>? = null
        ) {
            var type = "EXPORT_RATINGS";

            fun totalCount() =
                    (tracksByMSID?.size ?: 0) +
                    (tracksByName?.size ?: 0) +
                    (albumsByMSID?.size ?: 0) +
                    (albumsByName?.size ?: 0) +
                    (artistsByMSID?.size ?: 0) +
                    (artistsByName?.size ?: 0) +
                    (playlistsByName?.size ?: 0);

        }

    }
    @SettingsGroup("Media", 2)
    var media = MediaSettings();


    @Serializable
    class ShuffleSettings {

        @Setting("Shuffle Re-occurrence", "How many tracks have to pass before a track re-occurs in smart shuffle.", order = 8, type = SettingType.DROPDOWN)
        @SettingDropdownOptions(shuffle_reoccurrence)
        public var shuffleReoccurrenceTime = 0;

        @Setting("Shuffle Re-occurrence Chance", "How likely it is for a track to re-occur after re-occurrence is allowed.", order = 9, type = SettingType.DROPDOWN)
        @SettingDropdownOptions(shuffle_reoccurrence_time)
        public var shuffleReoccurrenceChance = 3;


        fun getShuffleReoccurenceTime(): Int {
            return when(shuffleReoccurrenceTime) {
                0 -> -1
                1 -> 3
                2 -> 5
                3 -> 7
                4 -> 10
                5 -> 20
                else -> -1
            }
        }
        fun getShuffleReoccurenceChanceTurns(): Int{
            return when(shuffleReoccurrenceChance) {
                0 -> 0
                1 -> 3
                2 -> 5
                3 -> 10
                else -> 0
            }
        }

        fun getShuffleAlgorithm(scores: ScoreContainer): SmartShuffle {
            return when(instance.developer.shuffleAlgorithm) {
                0 -> getLatestShuffleAlgorithm(scores);
                1 -> getLatestShuffleAlgorithm(scores);
                2 -> ESmartShuffle(scores);
                3 -> ESmartShuffleV2(scores);
                else -> getLatestShuffleAlgorithm(scores);
            }
        }
        fun getLatestShuffleAlgorithm(scores: ScoreContainer) = ESmartShuffleV2(scores);

    }
    @SettingsGroup("Shuffle", 3)
    var shuffle = ShuffleSettings();


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

        @Setting("Show Algorithm Name", "Name the smart shuffle queue after the algorithm", order = 2, type = SettingType.TOGGLE)
        public var showAlgorithmName: Boolean = false;
        @Setting("Smart Shuffle Algorithm", description = "Which smart algorithm to use", order = 3, type = SettingType.DROPDOWN)
        @SettingDropdownOptions(shuffle_algorithm)
        public var shuffleAlgorithm = 0;

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
                        StateDatabase.instance.db.tracksDao().setRating(DBTrackUpdateRatingDone(id, 0, false));
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



        @Setting("Temp Action", "Used for running temp code. Don't use if you didn't write the code.")
        fun tempAction() {
            GlobalScope.launch(Dispatchers.IO) {
                val allRoots = StateDatabase.instance.db.filesDao().getAllIds().map { it.rootId }.distinct();
                for (rootId in allRoots) {
                    UIDialogs.appToast("Deleting rootID files: ${rootId}");
                    StateDatabase.instance.db.filesDao().deleteRoot(rootId);
                }
            }
        }
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
