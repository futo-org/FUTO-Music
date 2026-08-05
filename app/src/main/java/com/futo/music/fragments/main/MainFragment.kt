package com.futo.music.fragments.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.futo.music.activities.MainActivity
import com.futo.music.constructs.Event1
import com.futo.music.fragments.MainActivityFragment
import com.futo.music.fragments.bottom.BotFragment
import com.futo.music.fragments.top.TopFragment

abstract class MainFragment : MainActivityFragment() {
    open val isMainView: Boolean = false;
    open val isTab: Boolean = false;
    open val isOverlay: Boolean = false;
    open val isHistory: Boolean = true;
    open val hasParameterHistory: Boolean = true;
    open val hasBottomBar: Boolean get() { return botBar != null };
    var topBar: TopFragment? = null;
    var botBar: BotFragment? = null;

    open val fragmentTitle: String? = null;

    val onShownEvent = Event1<MainFragment>();
    val onHideEvent = Event1<MainFragment>();
    val onCloseEvent = Event1<MainFragment>();

    private val _fragmentLock = Object();
    private var _mainView: View? = null;
    private var _lastOnShownParameters: Pair<Any?, Boolean>? = null;

    open fun onShown(parameter: Any?, isBack: Boolean) {
        onShownEvent.emit(this);

        if (_mainView == null) {
            synchronized(_fragmentLock) {
                _lastOnShownParameters = Pair(parameter, isBack);
            }
        } else {
            synchronized(_fragmentLock) {
                _lastOnShownParameters = null;
            }

            onShownWithView(parameter, isBack);
        }
    }

    open fun onShownWithView(parameter: Any?, isBack: Boolean) {

    }

    open fun onBackPressed(): Boolean {
        return false;
    }

    open fun onHide() {
        onHideEvent.emit(this);
    }

    final override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = onCreateMainView(inflater, container, savedInstanceState);
        _mainView = view;

        val lastOnShownParameters = synchronized(_fragmentLock) {
            val value = _lastOnShownParameters;
            _lastOnShownParameters = null;
            return@synchronized value;
        };

        if (lastOnShownParameters != null)
            onShownWithView(lastOnShownParameters.first, lastOnShownParameters.second);

        return view;
    }

    final override fun onDestroyView() {
        super.onDestroyView();
        onDestroyMainView();
        _mainView = null;
    }

    abstract fun onCreateMainView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View;
    open fun onDestroyMainView() {}

    override fun onDestroy() {
        super.onDestroy();
        onShownEvent.clear();
        onHideEvent.clear();
        onCloseEvent.clear();
    }

    fun close(withNavigate: Boolean = false) {
        isValidMainActivity();
        onCloseEvent.emit(this);
        if (withNavigate)
            (activity as MainActivity).closeSegment(this);
    }

}