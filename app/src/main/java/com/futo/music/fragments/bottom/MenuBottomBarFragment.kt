package com.futo.music.fragments.bottom

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.futo.music.R
import com.futo.music.UIDialogs
import com.futo.music.fragments.main.FilesFragment
import com.futo.music.fragments.main.HomeFragment
import com.futo.music.fragments.main.MainFragment
import com.futo.music.fragments.main.PlaybackFragment
import com.futo.music.fragments.main.SearchFragment
import com.futo.music.fragments.main.SettingsFragment
import com.futo.music.logging.Logger
import com.futo.music.logic.PlayerManager
import com.futo.music.models.ImageVariable
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
            private val _buttonSearch: MenuBottomButton;
            private val _buttonFiles: MenuBottomButton;
            private val _buttonSettings: MenuBottomButton?;

            private val _buttonsMenu: List<MenuBottomButton>



            val containerProgress: ConstraintLayout;
            val textProgressTitle: TextView;
            val textProgressDescription: TextView;
            val progressAnn: android.widget.ProgressBar;
            val imageProgress: ImageView;

            constructor(
                fragment: MenuBottomBarFragment,
                inflater: LayoutInflater
            ) : super(inflater.context) {
                _fragment = fragment;
                _inflater = inflater;
                inflater.inflate(R.layout.fragment_overview_bottom_bar, this);

                _buttonHome = findViewById(R.id.button_home);
                _buttonSearch = findViewById(R.id.button_search);
                _buttonFiles = findViewById(R.id.button_files);
                _buttonSettings = findViewById(R.id.button_settings);
                _buttonsMenu = listOf(_buttonHome, _buttonSearch, _buttonFiles);


                containerProgress = findViewById(R.id.progress_container);
                textProgressTitle = findViewById(R.id.text_progress_title);
                textProgressDescription = findViewById(R.id.text_progress_description);
                progressAnn = findViewById(R.id.progress_ann);
                imageProgress = findViewById(R.id.progress_image);

                _buttonHome.onClick.subscribe {
                    _buttonsMenu.forEach { it.setActive(false) };
                    fragment.navigate<HomeFragment>();
                }
                _buttonSearch?.onClick?.subscribe {
                    _buttonsMenu.forEach { it.setActive(false) };
                    fragment.navigate<SearchFragment>();
                }
                _buttonFiles.onClick.subscribe {
                    _buttonsMenu.forEach { it.setActive(false) };
                    //UIDialogs.toast("Files implementation pending");
                    fragment.navigate<FilesFragment>();
                }
                _buttonSettings?.onClick?.subscribe {
                    _buttonsMenu.forEach { it.setActive(false) };
                    fragment.navigate<SettingsFragment>();
                }

                _playerPeek = findViewById(R.id.player_peek);
                _progress = findViewById(R.id.progress);
                _progress.inactiveColor = Color.TRANSPARENT;

                _playerPeek.onClick.subscribe {
                    fragment.navigate<PlaybackFragment>();
                }
            }

            override fun onAttachedToWindow() {
                super.onAttachedToWindow();checkForProgressAnnouncement();
                checkForProgressAnnouncement();
                StateAnnouncement.instance.onAnnouncementChanged.subscribe(this) {
                    checkForProgressAnnouncement();
                }
            }
            override fun onDetachedFromWindow() {
                super.onDetachedFromWindow();
                StateAnnouncement.instance.onAnnouncementChanged.remove(this);
            }

            fun updateBottomMenuState(currentFragment: MainFragment) {
                if(currentFragment is HomeFragment) {
                    _buttonsMenu.forEach { it.setActive(false) };
                    _buttonHome.setActive(true);
                }
                else if(currentFragment is FilesFragment) {
                    _buttonsMenu.forEach { it.setActive(false) };
                    _buttonFiles.setActive(true);
                }
                else if(currentFragment is SettingsFragment) {
                    _buttonsMenu.forEach { it.setActive(false) };
                    _buttonSettings?.setActive(true);
                }
                else if(currentFragment is SearchFragment) {
                    _buttonsMenu.forEach { it.setActive(false) };
                    _buttonSearch?.setActive(true);
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





            fun showProgress(title: String, description:String, progressVal: Float, icon: ImageVariable? = null) {
                val isVisible = containerProgress.isVisible;
                if(!isVisible)
                    containerProgress.isVisible = true;
                textProgressTitle.text = title;
                textProgressDescription.text = description;
                if(icon?.resId != null)
                    imageProgress.setImageResource(icon.resId);
                progressAnn.isIndeterminate = progressVal <= 0f || progressVal >= 1f;
                progressAnn.progress = (progressVal * 100).toInt();
            }
            fun hideProgress() {
                containerProgress.visibility = View.GONE;
            }

            private var shownAnnouncement: SessionAnnouncement? = null;
            fun checkForProgressAnnouncement() {
                if(shownAnnouncement?.isRemoved ?: false)
                    return;
                val announcementWithProgress = StateAnnouncement.instance.getVisibleAnnouncements().find { it is SessionAnnouncement && it.progress != null } as SessionAnnouncement?;
                if(announcementWithProgress != null) {
                    shownAnnouncement = announcementWithProgress;
                    var lastUpdate = System.currentTimeMillis();
                    announcementWithProgress?.onProgressChanged?.subscribe(this) {
                        if(shownAnnouncement == announcementWithProgress) {
                            val nowMs = System.currentTimeMillis();
                            if(nowMs - lastUpdate > 100) {
                                lastUpdate = nowMs;
                                _fragment.lifecycleScope.launch(Dispatchers.Main) {
                                    showProgress(announcementWithProgress.title, announcementWithProgress.progressText ?: "", announcementWithProgress.progress?.toFloat() ?: -1f);
                                }
                            }
                        }
                    }
                    _fragment.lifecycleScope.launch(Dispatchers.Main) {
                        showProgress(announcementWithProgress?.title ?: "", announcementWithProgress?.progressText ?: "", announcementWithProgress.progress?.toFloat() ?: -1f, announcementWithProgress.icon);
                    }
                }
                else
                    _fragment.lifecycleScope.launch(Dispatchers.Main) {
                        hideProgress();
                    }
            }
        }
}