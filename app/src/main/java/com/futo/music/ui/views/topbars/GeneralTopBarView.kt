package com.futo.music.ui.views.topbars

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.futo.music.R
import com.futo.music.fragments.main.MainFragment
import com.futo.music.fragments.main.NotificationOverlayView
import com.futo.music.fragments.main.SettingsFragment
import com.futo.music.states.StateAnnouncement
import com.futo.music.states.StateApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeneralTopBarView: ConstraintLayout {

    private val _buttonNotifs: ConstraintLayout;
    private val _buttonNotifIcon: ImageView;
    private val _buttonNotifCount: TextView;
    private val _textTitle: TextView;
    private val _textMiniMeta: TextView;

    private val _buttonSettings: ConstraintLayout;
    private val _buttonSettingsIcon: ImageView;

    private var _parentFragment: MainFragment? = null;

    init {
    }

    constructor(context: Context, attrs: AttributeSet? = null): super(context, attrs) {
        inflate(context, R.layout.fragment_overview_top_bar, this);

        _textTitle = findViewById(R.id.text_title)
        _buttonNotifs = findViewById(R.id.button_notifs);
        _buttonNotifIcon = findViewById(R.id.button_notifs_icon);
        _buttonNotifCount = findViewById(R.id.button_notifs_count);
        _textMiniMeta = findViewById(R.id.text_minimeta);

        _buttonSettings = findViewById(R.id.button_settings)
        _buttonSettingsIcon = findViewById(R.id.button_settings_icon);


        _buttonNotifs.setOnClickListener {
            val frag = _parentFragment;
            if(frag != null) {
                if(frag is NotificationOverlayView.Frag)
                    frag.closeSegment();
                else
                    frag.navigate<NotificationOverlayView.Frag>();
            }
            else
                StateApp.instance.activity()?.navigate<NotificationOverlayView.Frag>();
        }

        _buttonSettings.setOnClickListener {
            val frag = _parentFragment;
            if(frag != null) {
                if(frag is SettingsFragment)
                    frag.closeSegment();
                else
                    frag.navigate<SettingsFragment>();
            }
            else
                StateApp.instance.activity()?.navigate<SettingsFragment>();
        }

        if(attrs != null) {

            val attrArr = context.obtainStyledAttributes(attrs, R.styleable.GeneralTopBarView, 0, 0);
            val title = attrArr.getString(R.styleable.GeneralTopBarView_GeneralTopBar_text);

            if(!title.isNullOrBlank())
                setTitle(title);
            else
                setTitle("");
        }
        else
            setTitle("")


        updateNotifCount();
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow();
        StateAnnouncement.instance.onAnnouncementChanged.subscribe(this) {
            (_parentFragment?.lifecycleScope ?: StateApp.instance.activity()?.lifecycleScope)?.launch(Dispatchers.Main) {
                updateNotifCount();
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow();
        StateAnnouncement.instance.onAnnouncementChanged.remove(this);
    }

    fun setFragment(parentFrag: MainFragment) {
        _parentFragment = parentFrag;

        if(_parentFragment is NotificationOverlayView.Frag)
            _buttonNotifIcon.setImageResource(R.drawable.ic_notifications_filled);
        else
            _buttonNotifIcon.setImageResource(R.drawable.ic_notifications);
    }

    fun setTitleMini(str: String) {
        _textMiniMeta.text = str;
    }

    fun setTitlePress(handler: (()->Unit)?) {
        if(handler == null)
            _textTitle?.setOnClickListener { };
        else
            _textTitle?.setOnClickListener { handler!!(); };
    }

    fun setTitleLongPress(handler: (()->Unit)?) {
        if(handler == null)
            _textTitle?.setOnLongClickListener { return@setOnLongClickListener false};
        else
            _textTitle?.setOnLongClickListener { handler!!(); return@setOnLongClickListener true };
    }

    fun updateNotifCount() {
        val currentAnnouncements = StateAnnouncement.instance.getVisibleAnnouncements();
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

    fun setTitle(title: String) {
        _textTitle.text = title;
    }
}