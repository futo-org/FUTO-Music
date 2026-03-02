package com.futo.music.fragments.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.futo.music.R
import com.futo.music.fragments.MainFragView

class TemplateFragment: MainFragment() {
    override val isMainView : Boolean = true;
    override val isTab: Boolean = true;
    override val hasBottomBar: Boolean get() = true;


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


    class FragView(frag: TemplateFragment, inflater: LayoutInflater): MainFragView<TemplateFragment>(frag, inflater) {

        init {

        }


        fun onShown(paramter: Any? = null) {

        }

        fun onHide() {
            
        }
    }
}