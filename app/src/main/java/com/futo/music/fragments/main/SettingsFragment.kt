package com.futo.music.fragments.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.collection.emptyLongSet
import androidx.compose.animation.core.updateTransition
import androidx.core.view.isVisible
import com.futo.music.R
import com.futo.music.UIDialogs
import com.futo.music.activities.MainActivity
import com.futo.music.fragments.MainFragView
import com.futo.music.models.playable.IPlayable
import com.futo.music.states.ArtistOrdering
import com.futo.music.states.StateLibrary
import com.futo.music.ui.views.containers.ContentGrid
import com.futo.music.ui.views.general.SearchBarView
import com.futo.music.ui.views.topbars.NavigationTopBarView

class SettingsFragment: MainFragment() {
    override val isMainView : Boolean = true;
    override val isTab: Boolean = true;
    override val hasBottomBar: Boolean get() = true;

    override val fragmentTitle: String = "Settings";

    private var _view: FragView? = null;


    override fun onShownWithView(parameter: Any?, isBack: Boolean) {
        super.onShownWithView(parameter, isBack);
        _view?.onShown(parameter);
    }

    override fun onHide() {
        super.onHide();
        _view?.onHide();
    }

    override fun onCreateMainView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = FragView(this, inflater);
        _view = view;
        return view;
    }
    override fun onDestroyMainView() {
        super.onDestroyMainView();
        _view = null;
    }


    class FragView(frag: SettingsFragment, inflater: LayoutInflater): MainFragView<SettingsFragment>(frag, inflater, R.layout.fragment_settings) {


        init {
            findViewById<NavigationTopBarView>(R.id.topbar).apply {
                setFragment(frag);
            }
        }


        fun onShown(paramter: Any? = null) {
        }

        fun onHide() {

        }
    }
}