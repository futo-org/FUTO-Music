package com.futo.music.fragments.top

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.futo.music.R
import com.futo.music.fragments.main.MainFragment
import com.futo.music.states.StateApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NavigationTopBarFragment : TopFragment() {

    private var _buttonNotifs: ConstraintLayout? = null;
    private var _buttonBack: ImageButton? = null;
    private var _buttonNotifIcon: ImageView? = null;
    private var _buttonNotifCount: TextView? = null;
    private var _textTitle: TextView? = null;

    private var _buttonSettings: ConstraintLayout? = null;
    private var _buttonSettingsIcon: ImageView? = null;

    private var _buttonGeneral: ConstraintLayout? = null;
    private var _buttonGeneralIcon: ImageView? = null;

    private var _title: String = "";

    fun updateNotifCount() {
        val currentAnnouncements = listOf<Any>()//StateAnnouncement.instance.getVisibleAnnouncements();
        if(currentAnnouncements.any())
            _buttonNotifCount?.let {
                it.text = currentAnnouncements.size.toString();
                it.visibility = View.VISIBLE;
            }
        else
            _buttonNotifCount?.let {
                it.text = currentAnnouncements.size.toString();
                it.visibility = View.GONE;
            }

        _buttonNotifIcon?.let {
            it.isVisible = false;
        }
        _buttonNotifCount?.let {
            it.isVisible = false;
        }
        _buttonSettings?.let {
            it.isVisible = false;
        }
    }

    override fun onShown(parameter: Any?) {
        _buttonGeneral?.isVisible = false;
        _buttonGeneral?.setOnClickListener {  };
    }
    override fun onHide() {
        _buttonGeneral?.isVisible = false;
        _buttonGeneral?.setOnClickListener {  };
    }

    override fun onShowFragment(frag: MainFragment) {
        super.onShowFragment(frag);
        _title = frag.fragmentTitle ?: "";
        _textTitle?.let {
            it.text = _title;
        }
    }

    fun setTitle(title: String) {
        _title = title;
        _textTitle?.let {
            it.text = title;
        }
    }

    fun setGeneralButton(icon: Int, handler: ()->Unit) {
        _buttonGeneralIcon?.setImageResource(icon);
        _buttonGeneral?.setOnClickListener {
            handler();
        }
        _buttonGeneral?.isVisible = true;
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_navigation_top_bar, container, false);

        _textTitle = view.findViewById(R.id.text_title)
        _buttonNotifs = view.findViewById(R.id.button_notifs);
        _buttonBack = view.findViewById(R.id.button_back);
        _buttonNotifIcon = view.findViewById(R.id.button_notifs_icon);
        _buttonNotifCount = view.findViewById(R.id.button_notifs_count);
        _buttonSettings = view.findViewById(R.id.button_settings);
        _buttonSettingsIcon = view.findViewById(R.id.button_settings_icon);
        _buttonGeneral = view.findViewById(R.id.button_general);
        _buttonGeneralIcon = view.findViewById(R.id.button_general_icon);


        _buttonBack?.setOnClickListener {
            if(StateApp.instance.activity()?.fragCurrent?.onBackPressed() ?: false)
                return@setOnClickListener;
            closeSegment();
        }


        _textTitle?.let {
            it.text = _title;
        }
        
        updateNotifCount();

        return view;
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _textTitle = null;
    }
}