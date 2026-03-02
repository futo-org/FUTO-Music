package com.futo.music.fragments.bottom

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.futo.music.R
import com.futo.music.fragments.MainActivityFragment
import com.futo.music.fragments.main.PlaybackFragment
import com.futo.music.logic.PlayerManager
import com.futo.music.ui.views.playback.PlaybackPeekView

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

            constructor(
                fragment: MenuBottomBarFragment,
                inflater: LayoutInflater
            ) : super(inflater.context) {
                _fragment = fragment;
                _inflater = inflater;
                inflater.inflate(R.layout.fragment_overview_bottom_bar, this);

                _playerPeek = findViewById(R.id.player_peek);

                _playerPeek.onClick.subscribe {
                    fragment.navigate<PlaybackFragment>();
                }
            }

            fun setPlayer(player: PlayerManager) {
                _playerPeek.setPlayer(player);
            }

            fun cleanup() {

            }

            fun onBackPressed(): Boolean {
                return false;
            }
        }
}