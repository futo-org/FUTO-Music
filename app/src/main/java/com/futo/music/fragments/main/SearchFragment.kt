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
import com.futo.music.UIDialogs
import com.futo.music.fragments.MainFragView
import com.futo.music.models.playable.IPlayable
import com.futo.music.states.ArtistOrdering
import com.futo.music.states.StateApp
import com.futo.music.states.StateDatabase
import com.futo.music.states.StateLibrary
import com.futo.music.ui.views.NoResultsView
import com.futo.music.ui.views.containers.ContentGrid
import com.futo.music.ui.views.general.SearchBarView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchFragment: MainFragment() {
    override val isMainView : Boolean = true;
    override val isTab: Boolean = true;
    override val hasBottomBar: Boolean get() = true;

    override val fragmentTitle: String = "Search";

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


    class FragView(frag: SearchFragment, inflater: LayoutInflater): MainFragView<SearchFragment>(frag, inflater, R.layout.fragment_search) {

        val search: SearchBarView;
        val gridSearch: ContentGrid;
        val emptyView: NoResultsView;


        init {
            search = findViewById(R.id.view_search);
            gridSearch = findViewById(R.id.grid_search);
            emptyView = findViewById(R.id.view_empty);

            gridSearch.onClick.subscribe {
                //fragment.navigate<PlaybackFragment>(it);
                UIDialogs.overlayPlayable(it);
                search.closeKeyboard(StateApp.instance.activity())
            }

            search.onChange.subscribe {
                updateContent(it);
            }
            search.onEnter.subscribe {
                search.closeKeyboard(fragment?.activity);
            }

            updateContent("");
        }

        fun updateContent(str: String) {
            if(str.length < 2) {
                emptyView.isVisible = true;
                gridSearch.isVisible = false;

            }
            else {
                emptyView.isVisible = false;
                gridSearch.isVisible = true;
                fragment.lifecycleScope.launch(Dispatchers.IO) {
                    val search = StateDatabase.instance.search(str);

                    withContext(Dispatchers.Main) {
                        if (search.isNullOrEmpty())
                            gridSearch.visibility = GONE;
                        else {
                            gridSearch.setData(search);
                            gridSearch.visibility = VISIBLE;
                        }
                    }
                }
            }

            //val artists = StateLibrary.instance.getArtists(fragment.requireContext(), ArtistOrdering.TrackCount).map { it.toArtist() } as List<IPlayable>;
            //val albums = StateLibrary.instance.getAlbums(fragment.requireContext()).map { it.toAlbum() } as List<IPlayable>;

        }

        fun onShown(parameter: Any? = null) {
            if(parameter != null && parameter is String && !parameter.isEmpty())
                search.setText(parameter);
            search.focus((fragment?.activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?));
        }

        fun onHide() {

        }
    }
}