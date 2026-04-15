package com.futo.music.fragments.main

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.collection.emptyLongSet
import androidx.compose.animation.core.updateTransition
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.futo.music.R
import com.futo.music.fragments.MainFragView
import com.futo.music.fragments.top.GeneralTopBarFragment
import com.futo.music.fragments.top.NavigationTopBarFragment
import com.futo.music.models.playable.IPlayable
import com.futo.music.states.ArtistOrdering
import com.futo.music.states.StateDatabase
import com.futo.music.states.StateLibrary
import com.futo.music.ui.views.NoResultsView
import com.futo.music.ui.views.containers.ContentGrid
import com.futo.music.ui.views.general.SearchBarView
import com.futo.music.ui.views.topbars.NavigationTopBarView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContentsFragment: MainFragment() {
    override val isMainView : Boolean = true;
    override val isTab: Boolean = true;
    override val hasBottomBar: Boolean get() = true;

    override val fragmentTitle: String
        get() = customTitle ?: "Contents";
    private var customTitle: String? = null;


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


    class FragView(frag: ContentsFragment, inflater: LayoutInflater): MainFragView<ContentsFragment>(frag, inflater, R.layout.fragment_contents) {

        val search: SearchBarView;
        val gridContent: ContentGrid;
        val emptyView: NoResultsView;

        var contents: List<IPlayable>? = null;

        val topbar: NavigationTopBarView;


        init {
            search = findViewById(R.id.view_search);
            gridContent = findViewById(R.id.grid_search);
            emptyView = findViewById(R.id.view_empty);
            topbar = findViewById(R.id.topbar);

            gridContent.onClick.subscribe {
                fragment.navigate<PlaybackFragment>(it);
            }
            search.onChange.subscribe {
                contents?.let { contents ->
                    val q = it.lowercase().trim();
                    if(it.isNotBlank())
                        updateContent(contents.filter { it.name.lowercase().contains(q) });
                    else
                        updateContent(contents);
                }
            }

            updateContent(listOf());
        }

        fun updateContent(contents: List<IPlayable>) {
            if(contents.size  == 0) {
                emptyView.isVisible = true;
                gridContent.isVisible = false;

            }
            else {
                emptyView.isVisible = false;
                gridContent.setData(contents);
                gridContent.isVisible = true;
            }

        }

        fun onShown(parameter: Any? = null) {
            if(parameter is List<*>) {
                contents = parameter.filterIsInstance<IPlayable>()
                updateContent(parameter.filterIsInstance<IPlayable>());
            }
            else if(parameter is Pair<*, *> && parameter.first is String && parameter.second is List<*>) {
                fragment.customTitle = parameter.first as String;
                fragment.topBar?.let {
                    if(it is GeneralTopBarFragment)
                        it.setTitle(parameter.first as String);
                }
                topbar.setTitle(parameter.first as String);
                contents = (parameter.second as List<*>).filterIsInstance<IPlayable>()
                updateContent((parameter.second as List<*>).filterIsInstance<IPlayable>());
            }
        }

        fun onHide() {

        }
    }
}