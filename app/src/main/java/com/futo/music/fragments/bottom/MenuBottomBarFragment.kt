package com.futo.music.fragments.bottom

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.futo.music.R
import com.futo.music.fragments.main.PlaybackFragment
import com.futo.music.logging.Logger
import com.futo.music.logic.PlayerManager
import com.futo.music.states.SessionAnnouncement
import com.futo.music.states.StateAnnouncement
import com.futo.music.ui.views.playback.PlaybackPeekView
import com.futo.music.ui.views.progress.ProgressBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class MenuBottomBarFragment : BotFragment() {
    private var _view: MenuBottomBarView? = null;
    private var _player: PlayerManager? = null;

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = MenuBottomBarView(this, inflater);
        _player?.let {
            view.setPlayer(it);
        }
        _view = view;
        return view;
    }

    fun setPlayer(player: PlayerManager) {
        _player = player;
        _view?.setPlayer(player);
    }

    override fun onResume() {
        super.onResume();
    }

    override fun onDestroyView() {
        super.onDestroyView();

        _view?.cleanup();
        _view = null;
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig);
    }


        @SuppressLint("ViewConstructor")
    class MenuBottomBarView : LinearLayout {
            private val _fragment: MenuBottomBarFragment;
            private val _inflater: LayoutInflater;

            private val _playerPeek: PlaybackPeekView;

            private val _progress: ProgressBar;

            constructor(
                fragment: MenuBottomBarFragment,
                inflater: LayoutInflater
            ) : super(inflater.context) {
                _fragment = fragment;
                _inflater = inflater;
                inflater.inflate(R.layout.fragment_overview_bottom_bar, this);

                _playerPeek = findViewById(R.id.player_peek);
                _progress = findViewById(R.id.progress);
                _progress.inactiveColor = Color.TRANSPARENT;

                _playerPeek.onClick.subscribe {
                    fragment.navigate<PlaybackFragment>();
                }

                StateAnnouncement.instance.onAnnouncementChanged.subscribe(this) {
                    findNewProgressAnnouncement();
                }
                findNewProgressAnnouncement()
            }

            private var _announcement_vis: SessionAnnouncement? = null;
            private val _announcement_listeners = ConcurrentHashMap<SessionAnnouncement, Any>();
            fun findNewProgressAnnouncement() {
                try {
                    var lastAnnouncementDate = System.currentTimeMillis();
                    val announcements = StateAnnouncement.instance.getVisibleAnnouncements();

                    val announcement =
                        announcements.find { if (it is SessionAnnouncement) it.progress != null && !_announcement_listeners.containsKey(it) else false };
                    if (announcement is SessionAnnouncement) {
                        val obj = Any();
                        _announcement_listeners.put(announcement, obj)
                        announcement.onProgressChanged.subscribe(obj) {
                            val prog = it.progress ?: return@subscribe;
                            if ((System.currentTimeMillis() - lastAnnouncementDate) > 100 || !_progress.isVisible) {
                                _fragment.lifecycleScope.launch(Dispatchers.Main) {
                                    if (!_progress.isVisible)
                                        _progress.isVisible = true;
                                    _progress.progress = prog.toFloat();
                                    lastAnnouncementDate = System.currentTimeMillis();
                                    _announcement_vis = it;
                                }
                            } else if (prog == 1.00 || prog == 0.00) {
                                _fragment.lifecycleScope.launch(Dispatchers.Main) {
                                    _progress.isVisible = false;
                                }
                            }
                        }
                        announcement.onRemoved.subscribe {
                            if(_announcement_listeners.containsKey(it))
                            {
                                _announcement_listeners.remove(it);
                                if(_announcement_vis == it) {
                                    _fragment.lifecycleScope.launch(Dispatchers.Main) {
                                        _progress.isVisible = false;
                                        _announcement_vis = null;
                                    }
                                }
                            }
                        }
                    }
                } catch (ex: Throwable) {
                    Logger.e("MenuBottomBarFragment", "Failed progress update", ex);
                }
            }


            fun setPlayer(player: PlayerManager) {
                _playerPeek.setPlayer(player);
            }

            fun cleanup() {
                StateAnnouncement.instance.onAnnouncementChanged.remove(this);
            }

            fun onBackPressed(): Boolean {
                return false;
            }
        }
}