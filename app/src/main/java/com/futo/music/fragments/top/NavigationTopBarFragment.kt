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
import androidx.lifecycle.lifecycleScope
import com.futo.music.R
import com.futo.music.fragments.main.MainFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NavigationTopBarFragment : TopFragment() {

    private var _buttonNotifs: ConstraintLayout? = null;
    private var _buttonBack: ImageButton? = null;
    private var _buttonNotifIcon: ImageView? = null;
    private var _buttonNotifCount: TextView? = null;
    private var _textTitle: TextView? = null;

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
    }

    override fun onShown(parameter: Any?) {

    }
    override fun onHide() {

    }

    override fun onShowFragment(frag: MainFragment) {
        super.onShowFragment(frag);
        _title = frag.fragmentTitle ?: "";
        _textTitle?.let {
            it.text = _title;
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_navigation_top_bar, container, false);

        _textTitle = view.findViewById(R.id.text_title)
        _buttonNotifs = view.findViewById(R.id.button_notifs);
        _buttonBack = view.findViewById(R.id.button_back);
        _buttonNotifIcon = view.findViewById(R.id.button_notifs_icon);
        _buttonNotifCount = view.findViewById(R.id.button_notifs_count);

        _buttonBack?.setOnClickListener {
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