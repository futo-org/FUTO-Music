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
import com.futo.music.UIDialogs
import com.futo.music.fragments.main.HomeFragment
import com.futo.music.fragments.main.MainFragment
import com.futo.music.fragments.main.PlaybackFragment
import com.futo.music.fragments.main.SettingsFragment
import com.futo.music.logging.Logger
import com.futo.music.logic.PlayerManager
import com.futo.music.states.SessionAnnouncement
import com.futo.music.states.StateAnnouncement
import com.futo.music.states.StateApp
import com.futo.music.ui.buttons.MenuBottomButton
import com.futo.music.ui.views.playback.PlaybackPeekView
import com.futo.music.ui.views.progress.ProgressBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class MenuBottomBarFragment : BotFragment() {
    private var _view: MenuBottomBarView? = null;
    private var _player: PlayerManager? = null;

    private var _lastKnownView: MainFragment? = null;


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = MenuBottomBarView(this, inflater);
        _player?.let {
            view.setPlayer(it);
        }
        _lastKnownView?.let {
            view.updateBottomMenuState(it);
        }
        _view = view;
        return view;
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        StateApp.instance.activity()?.onNavigated?.subscribe(this) {
            updateBottomMenuState(it);
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        StateApp.instance.activity()?.onNavigated?.remove(this);
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

    fun updateBottomMenuState(frag: MainFragment) {
        _lastKnownView = frag;
        _view?.updateBottomMenuState(frag);
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

            private val _buttonHome: MenuBottomButton;
            private val _buttonFiles: MenuBottomButton;
            private val _buttonSettings: MenuBottomButton;

            private val _buttonsMenu: List<MenuBottomButton>

            constructor(
                fragment: MenuBottomBarFragment,
                inflater: LayoutInflater
            ) : super(inflater.context) {
                _fragment = fragment;
                _inflater = inflater;
                inflater.inflate(R.layout.fragment_overview_bottom_bar, this);

                _buttonHome = findViewById(R.id.button_home);
                _buttonFiles = findViewById(R.id.button_files);
                _buttonSettings = findViewById(R.id.button_settings);
                _buttonsMenu = listOf(_buttonHome, _buttonFiles, _buttonSettings);


                _buttonHome.onClick.subscribe {
                    _buttonsMenu.forEach { it.setActive(false) };
                    fragment.navigate<HomeFragment>();
                }
                _buttonFiles.onClick.subscribe {
                    _buttonsMenu.forEach { it.setActive(false) };
                    UIDialogs.toast("Files implementation pending");
                }
                _buttonSettings.onClick.subscribe {
                    _buttonsMenu.forEach { it.setActive(false) };
                    fragment.navigate<SettingsFragment>();
                }

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

            override fun onAttachedToWindow() {
                super.onAttachedToWindow();
            }
            override fun onDetachedFromWindow() {
                super.onDetachedFromWindow()
            }

            fun updateBottomMenuState(currentFragment: MainFragment) {
                if(currentFragment is HomeFragment) {
                    _buttonsMenu.forEach { it.setActive(false) };
                    _buttonHome.setActive(true);
                }
                else if(false) {
                    _buttonsMenu.forEach { it.setActive(false) };
                    _buttonFiles.setActive(true);
                }
                else if(currentFragment is SettingsFragment) {
                    _buttonsMenu.forEach { it.setActive(false) };
                    _buttonSettings.setActive(true);
                }
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