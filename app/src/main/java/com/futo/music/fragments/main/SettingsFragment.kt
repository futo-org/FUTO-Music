package com.futo.music.fragments.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.collection.emptyLongSet
import androidx.compose.animation.core.updateTransition
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.load.resource.bitmap.Rotate
import com.futo.music.R
import com.futo.music.UIDialogs
import com.futo.music.activities.MainActivity
import com.futo.music.fragments.MainFragView
import com.futo.music.fragments.general.AFragment
import com.futo.music.logging.Logger
import com.futo.music.models.playable.IPlayable
import com.futo.music.settings.Settings
import com.futo.music.states.ArtistOrdering
import com.futo.music.states.StateApp
import com.futo.music.states.StateLibrary
import com.futo.music.ui.adapters.TabAdapter
import com.futo.music.ui.adapters.TabDescriptor
import com.futo.music.ui.views.containers.ContentGrid
import com.futo.music.ui.views.containers.SettingsView
import com.futo.music.ui.views.general.SearchBarView
import com.futo.music.ui.views.topbars.NavigationTopBarView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import jp.wasabeef.glide.transformations.BlurTransformation

class SettingsFragment: MainFragment() {
    override val isMainView : Boolean = true;
    override val isTab: Boolean = true;
    override val hasBottomBar: Boolean get() = false;

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

        val viewPager: ViewPager2;
        val adapter: TabAdapter;
        val tabs: TabLayout;

        private var _settings: Settings;

        init {
            findViewById<NavigationTopBarView>(R.id.topbar).apply {
                setFragment(frag);
            }
            tabs = findViewById(R.id.tabs);
            viewPager = findViewById(R.id.viewpager);

            _settings = Settings.instance;

            val groups = _settings.getGroups();

            adapter = TabAdapter(fragment.childFragmentManager, fragment.lifecycle, groups.filter { _settings.developer.isDeveloper || it.second.name != "Developer" }.map { TabDescriptor(it.second.name){
                AFragment({ inflater, container ->
                    val sets = SettingsView(context) { view, setting ->
                        if(setting.name == "Version") {
                            if(view is View) {
                                view.setOnLongClickListener {
                                    UIDialogs.showConfirmDialog(fragment.requireContext(), R.drawable.ic_gear, "Enable Developer Mode?", "Do you want to enable developer settings?", {
                                        _settings.developer.isDeveloper = true;
                                        UIDialogs.appToast("Developer mode enabled");
                                        fragment.closeSegment();
                                    }, {
                                        _settings.developer.isDeveloper = false;
                                        UIDialogs.appToast("Developer mode disabled");
                                    });
                                    return@setOnLongClickListener true;
                                }
                            }
                        }
                    }
                    sets.setSettingsObject(it.first);
                    val scrollView = ScrollView(context);
                    scrollView.addView(sets);
                    return@AFragment scrollView;
                })
            }}.toMutableList());
            viewPager.adapter = adapter;
            viewPager.isSaveEnabled = false;
            val tabLayoutMediator = TabLayoutMediator(tabs, viewPager, adapter::getTabNames);
            tabLayoutMediator.attach();
        }


        fun onShown(paramter: Any? = null) {
            StateApp.instance.activity()?.setBackgroundTop(resources.getDrawable(R.drawable.background_glow), 2.5f, 0.5f, {
                it.transform(Rotate(180))
            });
        }

        fun onHide() {
            Logger.i("SettingsFragment", "On hide fragment");
            _settings.save();
        }
    }
}