package com.futo.music.states

import android.view.View
import android.view.WindowManager
import com.futo.music.constructs.Event0
import com.futo.music.constructs.Event1
import com.futo.music.logging.Logger
import com.futo.music.models.ImageVariable
import com.futo.music.serialization.OffsetDateTimeNullableSerializer
import com.futo.music.storage.file.FragmentedStorage
import com.futo.music.storage.file.StringHashSetStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.collections.filter
import kotlin.collections.filterKeys
import kotlin.collections.mapKeys
import kotlin.collections.mapValues
import kotlin.collections.plus
import kotlin.collections.set
import kotlin.collections.toList
import kotlin.let
import kotlin.text.isNullOrEmpty
import kotlin.text.toInt
import kotlin.text.toIntOrNull

class StateAnnouncement {
    private val _lock = Object();

    private val _sessionAnnouncementsNever = FragmentedStorage.get<StringHashSetStorage>("announcementNeverSession");
    private val _sessionAnnouncements: HashMap<String, Announcement> = hashMapOf();
    private val _sessionActions: HashMap<String, (announcement: Announcement) -> Unit> = hashMapOf();

    private val _announcementsNever = FragmentedStorage.get<StringHashSetStorage>("announcementNever");
    private val _announcementsStore = FragmentedStorage.storeJson<Announcement>("announcements").load();
    private val _announcementsClosed = kotlin.collections.HashSet<String>();

    val onAnnouncementChanged = Event0();

    suspend fun loadAnnouncements() {
        Logger.i(TAG, "Loading announcements")

        /*
        withContext(Dispatchers.IO) {
            try {
                val client = ManagedHttpClient();
                val response = client.get("https://announcements.grayjay.app/grayjay.json");
                if (response.isOk && response.body != null) {
                    val body = response.body.string();
                    val announcements = Json.decodeFromString<List<Announcement>>(body);

                    synchronized(_lock) {
                        for (announcement in announcements) {
                            if (_sessionAnnouncements.containsKey(announcement.id))
                                return@synchronized;

                            if (!_announcementsStore.hasItem { it.id == announcement.id }) {
                                _announcementsStore.saveAsync(announcement);
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        onAnnouncementChanged.emit();
                    }
                }
            } catch (e: Throwable) {
                Logger.w(TAG, "Failed to load announcements", e)
            }
        }*/

        Logger.i(TAG, "Finished loading announcements")
    }

    fun registerAnnouncement(id: String?, title: String, msg: String, announceType: AnnouncementType = AnnouncementType.SESSION, time: OffsetDateTime? = null, category: String? = null, icon: ImageVariable? = null, actionButton: String, action: ((announcement: Announcement)->Unit)) {
        synchronized(_lock) {
            val idActual = id ?: UUID.randomUUID().toString();
            val announcement = SessionAnnouncement(idActual, title, msg, announceType, time, category,actionButton, idActual, icon = icon);
            _sessionActions[idActual] = action;
            registerAnnouncementSession(announcement);
        }
    }
    fun registerAnnouncement(id: String?, title: String, msg: String, announceType: AnnouncementType = AnnouncementType.SESSION, time: OffsetDateTime? = null, category: String? = null, actionButton: String, action: ((announcement: Announcement)->Unit), cancelButton: String? = null, cancelAction: ((announcement: Announcement)-> Unit)? = null) {
        synchronized(_lock) {
            val idActual = id ?: UUID.randomUUID().toString();
            val announcement =
                SessionAnnouncement(idActual, title, msg, announceType, time, category, actionButton, idActual, cancelButton, if (cancelAction != null) idActual + "_cancel" else null);

            _sessionActions.put(idActual, action);
            if (cancelAction != null) {
                _sessionActions.put(idActual + "_cancel", cancelAction);
            }
            registerAnnouncementSession(announcement);
        }
    }
    fun registerAnnouncement(id: String?, title: String, msg: String, announceType: AnnouncementType = AnnouncementType.DELETABLE, time: OffsetDateTime? = null, category: String? = null, actionButton: String? = null, actionId: String? = null) {
        val newAnnouncement = Announcement(id ?: UUID.randomUUID().toString(), title, msg, announceType, time, category, actionButton, actionId);

        if(announceType == AnnouncementType.SESSION || announceType == AnnouncementType.SESSION_RECURRING) {
            registerAnnouncementSession(newAnnouncement);
        } else {
            registerAnnouncement(newAnnouncement);
        }
    }
    fun registerAnnouncementSession(announcement: Announcement) {
        synchronized(_lock) {
            _sessionAnnouncements.put(announcement.id, announcement);
        }

        onAnnouncementChanged.emit();
    }
    fun registerAnnouncement(announcement: Announcement) {
        synchronized(_lock) {
            if (_sessionAnnouncements.containsKey(announcement.id))
                return@synchronized;

            if (!_announcementsStore.hasItem { it.id == announcement.id }) {
                _announcementsStore.saveAsync(announcement);
            }
        }

        onAnnouncementChanged.emit();
    }

    //Special Announcement

    fun registerLoading(title: String, description: String, icon: ImageVariable? = null, customId: String? = null, withProgress: Boolean = false): SessionAnnouncement {
        val id = "loading-" + UUID.randomUUID().toString();
        val announcement = SessionAnnouncement(
            customId ?: id,
            title,
            description,
            AnnouncementType.ONGOING,
            null, "loading", null, null,
            null, null,null, icon
        );
        announcement.progress = if(withProgress) 0.0 else null;
        registerAnnouncementSession(announcement);
        return announcement;
    }



    fun getVisibleAnnouncements(category: String? = null): List<Announcement> {
        synchronized(_lock) {
            if (category != null) {
                return _announcementsStore.getItems().filter { it.category == category && !_announcementsNever.contains(it.id) && !_announcementsClosed.contains(it.id) } +
                        _sessionAnnouncements.values.filter { it.category == category && !_sessionAnnouncementsNever.contains(it.id) && !_announcementsClosed.contains(it.id) }
            } else {
                return _announcementsStore.getItems().filter { !_announcementsNever.contains(it.id) && !_announcementsClosed.contains(it.id) } +
                        _sessionAnnouncements.values.filter { !_sessionAnnouncementsNever.contains(it.id) && !_announcementsClosed.contains(it.id) }
            }
        }
    }

    fun closeAnnouncement(id: String?) {
        if(id == null)
            return;
        val item: Announcement?;
        synchronized(_lock) {
            item = _announcementsStore.findItem { it.id == id };

            if (item != null) {
                when (item.announceType) {
                    AnnouncementType.DELETABLE -> {
                        neverAnnouncement(item.id);
                    }

                    AnnouncementType.SESSION -> {
                        deleteAnnouncement(item.id);
                    }

                    else -> {
                        _announcementsClosed.add(item.id);
                    }
                }
            }
            val itemSession = _sessionAnnouncements.get(id);
            if (itemSession != null) {
                when (itemSession.announceType) {
                    AnnouncementType.DELETABLE -> {
                        neverAnnouncement(itemSession.id);
                    }

                    AnnouncementType.SESSION -> {
                        deleteAnnouncement(itemSession.id);

                        if (itemSession is SessionAnnouncement)
                            cancelActionAnnouncement(itemSession);
                    }

                    else -> {
                        _announcementsClosed.add(itemSession.id);
                    }
                }
            }
        }
        if(item is SessionAnnouncement) {
            if(item.cancelActionId != null) {
                val cancelAction = _sessionActions[item.cancelActionId];
                cancelAction?.invoke(item);
            }
        }
        onAnnouncementChanged?.emit();
    }

    fun deleteAllAnnouncements() {
        synchronized(_lock) {
            val items = _announcementsStore.getItems().toList();
            for (item in items) {
                _announcementsStore.delete(item);
            }

            val sessionItems = _sessionAnnouncements.toList();
            for (item in sessionItems) {
                _sessionAnnouncements.remove(item.first);
            }
        }

        onAnnouncementChanged.emit();
    }

    fun deleteAnnouncement(id: String) {
        var sessionAnnouncementRemoved: SessionAnnouncement? = null;
        synchronized(_lock) {
            val item = _announcementsStore.findItem { it.id == id };
            if (item != null)
                _announcementsStore.delete(item);
            val itemSession = _sessionAnnouncements.get(id);
            if (itemSession != null) {
                _sessionAnnouncements.remove(id);
                if(itemSession is SessionAnnouncement)
                    sessionAnnouncementRemoved = itemSession;
            }
        }
        if(sessionAnnouncementRemoved != null)
            sessionAnnouncementRemoved.onRemoved.emit(sessionAnnouncementRemoved);

        onAnnouncementChanged.emit();
    }
    fun neverAnnouncement(id: String?) {
        if(id == null)
            return;
        synchronized(_lock) {
            val item = _announcementsStore.findItem { it.id == id };
            if (item != null && !_announcementsNever.contains(id))
                _announcementsNever.add(id);
            val itemSession = _sessionAnnouncements.get(id);
            if (itemSession != null && !_sessionAnnouncementsNever.contains(id))
                _sessionAnnouncementsNever.add(id);
        }

        _sessionAnnouncementsNever.save();
        _announcementsNever.save();
        onAnnouncementChanged.emit();
    }
    fun actionAnnouncement(id: String?, extra: Boolean = false) {
        if(id == null)
            return;
        val item = _announcementsStore.findItem { it.id == id } ?: _sessionAnnouncements[id];
        if(item != null)
            actionAnnouncement(item, extra);
    }
    fun actionAnnouncement(item: Announcement, extra: Boolean = false) {
        val actionId = if(!extra) item.actionId else if(item is SessionAnnouncement) item.extraActionId else null;
        val actionData = if(!extra) item.actionData else if(item is SessionAnnouncement) item.extraActionData else null;

        val action = _sessionActions[item.id];
        if (action != null) {
            action(item);
        } else {
            when (actionId) {
                ACTION_NEVER -> neverAnnouncement(item.id);
                ACTION_SOMETHING -> actionSomething();
            }
        }
    }
    fun cancelActionAnnouncement(id: String) {
        val item = _announcementsStore.findItem { it.id == id } ?: _sessionAnnouncements[id];
        if(item != null)
            cancelActionAnnouncement(item);
    }
    fun cancelActionAnnouncement(item: Announcement) {
        if(item is SessionAnnouncement && item.cancelActionId != null) {
            val action = _sessionActions[item.cancelActionId];
            action?.invoke(item);
        }
    }

    fun resetAnnouncements() {
        _announcementsClosed.clear();
        _announcementsNever.values.clear();
        _announcementsNever.save();
        _sessionAnnouncementsNever.values.clear();
        _sessionAnnouncementsNever.save();
        _sessionAnnouncements.clear();
        onAnnouncementChanged.emit();
    }

    //TODO Actions
    private fun actionSomething() {

    }

    companion object {
        private var _instance: StateAnnouncement? = null;
        val instance: StateAnnouncement
            get(){
                if(_instance == null)
                    _instance = StateAnnouncement();
                return _instance!!;
            };


        const val ACTION_SOMETHING = "SOMETHING";
        const val ACTION_NEVER = "NEVER";
        private const val TAG = "StateAnnouncement";
    }
}

@Serializable
open class Announcement(
    val id: String,
    val title: String,
    var msg: String,
    val announceType: AnnouncementType,
    @Serializable(with = OffsetDateTimeNullableSerializer::class)
    val time: OffsetDateTime? = null,
    val category: String? = null,
    val actionName: String? = null,
    val actionId: String? = null,
    val actionData: String? = null
);
class SessionAnnouncement(
    id: String,
    title: String,
    msg: String,
    announceType: AnnouncementType,
    time: OffsetDateTime? = null,
    category: String? = null,
    actionName: String? = null,
    actionId: String? = null,
    val cancelName: String? = null,
    val cancelActionId: String? = null,
    actionData: String? = null,
    val icon: ImageVariable? = null
): Announcement(
    id= id,
    title = title,
    msg = msg,
    announceType = announceType,
    time = time,
    category = category,
    actionName = actionName,
    actionId = actionId,
    actionData = actionData
) {
    var extraActionName: String? = null;
    var extraActionId: String? = null;
    var extraActionData: String? = null;

    var extraObj: Any? = null;

    var progressText: String? = null;
    var progress: Double? = null;
    var isRemoved: Boolean = false;
    val onProgressChanged = Event1<SessionAnnouncement>();
    val onRemoved = Event1<SessionAnnouncement>();

    fun withExtraAction(name: String, id: String, data: String? = null): SessionAnnouncement {
        extraActionName = name;
        extraActionId = id;
        extraActionData = data;
        return this;
    }

    fun withProgress(): SessionAnnouncement{
        progress = 0.0;
        return this;
    }

    fun setProgress(progress: Double, progressText: String? = null) {
        this.progress = progress;
        if(progressText != null) {
            this.progressText = progressText;
            this.msg = progressText;
        }
        onProgressChanged?.emit(this);
    }
    fun setProgress(progress: Int, progressText: String? = null) {
        this.progress = progress.toDouble().div(100);
        if(progressText != null) {
            this.progressText = progressText;
            this.msg = progressText;
        }
        onProgressChanged?.emit(this);
    }
}

enum class AnnouncementType(val value : Int) {
    DELETABLE(0), //Close button deletes announcement (generally for actions)
    RECURRING(1), //Shows up till never is pressed (generally for patchnotes etc)
    PERMANENT(2), //Shows up until deleted through other means (action)
    SESSION(3), //Not persistent, only during this session
    SESSION_RECURRING(4), //Not persistent, only during this session, recurring id
    ONGOING(5);
}