package com.futo.music.fragments.main

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import androidx.collection.emptyLongSet
import androidx.compose.animation.core.updateTransition
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.futo.music.R
import com.futo.music.fragments.MainFragView
import com.futo.music.fragments.top.GeneralTopBarFragment
import com.futo.music.fragments.top.NavigationTopBarFragment
import com.futo.music.models.playable.IPlayable
import com.futo.music.openPlayable
import com.futo.music.settings.Settings
import com.futo.music.sort
import com.futo.music.states.ArtistOrdering
import com.futo.music.states.StateDatabase
import com.futo.music.states.StateLibrary
import com.futo.music.storage.db.DBTrack
import com.futo.music.ui.views.NoResultsView
import com.futo.music.ui.views.containers.ContentGrid
import com.futo.music.ui.views.general.SearchBarView
import com.futo.music.ui.views.general.SortDropdown
import com.futo.music.ui.views.general.SortDropdownType
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
        val dropdownSort: SortDropdown;
        val toggleList: ImageButton;

        var contents: List<IPlayable>? = null;

        val topbar: NavigationTopBarView;

        private var _lastPlayables: List<IPlayable>? = null;
        private var _selectedSort: SortDropdownType = SortDropdownType.CountDesc;

        private var _alwaysOptions = false;

        init {
            search = findViewById(R.id.view_search);
            gridContent = findViewById(R.id.grid_search);
            emptyView = findViewById(R.id.view_empty);
            topbar = findViewById(R.id.topbar);
            dropdownSort = findViewById(R.id.dropdown_sort);
            dropdownSort.setSelected(_selectedSort);
            dropdownSort.onSelectedChanged.subscribe {
                _selectedSort = it;
                _lastPlayables?.let {
                    updateContent(it);
                }
            }
            toggleList = findViewById(R.id.toggle_list);
            toggleList.setOnClickListener {
                setListView(!gridContent.gridSettings.listView);
            }

            gridContent.onClick.subscribe {
                //fragment.navigate<PlaybackFragment>(it);
                it.openPlayable(fragment, _alwaysOptions);
            }
            gridContent.onLongClick.subscribe {
                it.openPlayable(fragment, true);
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
            search.setBackgroundResource(R.drawable.background_bar_transparant_71_light_round_4dp);

            updateContent(listOf());
        }

        fun setListView(enabled: Boolean) {
            toggleList.setImageResource(if(enabled)R.drawable.ic_list else R.drawable.ic_grid_view);
            gridContent.setListView(enabled);
        }

        fun updateContent(contents: List<IPlayable>) {
            val newContents = sort(contents);
            _lastPlayables = newContents;
            if(contents.size  == 0) {
                emptyView.isVisible = true;
                gridContent.isVisible = false;

            }
            else {
                emptyView.isVisible = false;
                gridContent.setData(newContents);
                gridContent.isVisible = true;
            }

        }
        fun sort(contents: List<IPlayable>): List<IPlayable> {
            return contents.sort(_selectedSort);
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
            else if(parameter is Triple<*, *, *> && parameter.first is String && parameter.second is List<*> && parameter.third is List<*>) {
                fragment.customTitle = parameter.first as String;
                fragment.topBar?.let {
                    if(it is GeneralTopBarFragment)
                        it.setTitle(parameter.first as String);
                }
                topbar.setTitle(parameter.first as String);
                contents = (parameter.second as List<*>).filterIsInstance<IPlayable>()
                _alwaysOptions = (parameter.third as List<*>).contains(1);
                if((parameter.third as List<*>).contains(FLAG_MOST_PLAYED))
                    gridContent.gridSettings.showPlays = true;
                updateContent((parameter.second as List<*>).filterIsInstance<IPlayable>());
            }
            else if(parameter is Parameters) {
                if(parameter.title?.isNotBlank() ?: false) {
                    fragment.customTitle = parameter.title;
                    fragment.topBar?.let {
                        if (it is GeneralTopBarFragment)
                            it.setTitle(parameter.title)
                    }
                    topbar.setTitle(parameter.title);
                }
                contents = parameter.collection;
                _alwaysOptions = parameter.flags?.contains(1) ?: false;
                updateContent(parameter.collection);
                if(parameter?.flags?.contains(FLAG_MOST_PLAYED) ?: false)
                    gridContent.gridSettings.showPlays = true;
                if(parameter?.flags?.contains(FLAG_LIST) ?: false)
                    setListView(true)
                else
                    setListView(Settings.instance.general.preferListView);
            }
            contents?.let {
                if(it.all { it is DBTrack }) {
                    dropdownSort.setFilters(SortDropdown.OPTIONS.filter { it != SortDropdownType.Count && it != SortDropdownType.CountDesc });
                }
                else {
                    dropdownSort.setFilters(SortDropdown.OPTIONS);
                }
            }
            if(parameter is Parameters) {
                if(parameter.sortOrder != null) {
                    _selectedSort = parameter.sortOrder;
                    dropdownSort.setSelected(parameter.sortOrder);
                }
            }
        }

        fun onHide() {

        }
    }

    class Parameters(
        val collection: List<IPlayable>,
        val title: String? = null,
        val flags: List<Int>? = null,
        val sortOrder: SortDropdownType? = null
    )

    companion object {
        val FLAG_ALWAYS_OPTIONS = 1;
        val FLAG_MOST_PLAYED = 2;
        val FLAG_LIST = 3;
    }
}