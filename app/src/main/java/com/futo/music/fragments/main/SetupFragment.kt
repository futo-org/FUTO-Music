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

class SetupFragment: MainFragment() {
    override val isMainView : Boolean = true;
    override val isTab: Boolean = true;
    override val hasBottomBar: Boolean get() = false;

    override val fragmentTitle: String = "Home";

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


    class FragView(frag: SetupFragment, inflater: LayoutInflater): MainFragView<SetupFragment>(frag, inflater, R.layout.fragment_setup) {

        val buttonNext: Button;
        val textDenied: TextView;


        init {
            buttonNext = findViewById(R.id.button_next);
            textDenied = findViewById(R.id.text_denied);

            buttonNext.isVisible = false;
            textDenied.isVisible = false;

            buttonNext.setOnClickListener {
                fragment.activity?.let {
                    if(it is MainActivity) {

                        fragment.navigate<HomeFragment>();
                        /*
                        it.requestPermissionAudio {
                            if(it)
                                fragment.navigate<HomeFragment>();
                            else
                                updateState();
                        };*/
                    }
                }
            }
            updateState();
        }

        fun updateState() {
            fragment.activity?.let {
                if(it is MainActivity) {
                    val act = it;
                    it.requestPermissionAudio {
                        if(it) {
                            act.sync();
                            fragment.navigate<HomeFragment>();
                        }
                        else {
                            UIDialogs.appToast("Permissions were denied.\nPlease allow them in app-permissions.");
                            buttonNext.isVisible = true;
                            textDenied.isVisible = true;
                            buttonNext.text = "Continue without Permissions";
                        }
                    };
                }
            }
        }

        fun onShown(paramter: Any? = null) {
            updateState();
        }

        fun onHide() {

        }
    }
}