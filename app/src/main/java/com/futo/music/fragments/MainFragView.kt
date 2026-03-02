package com.futo.music.fragments

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.futo.music.fragments.main.MainFragment

abstract class MainFragView<T>: ConstraintLayout where T: MainFragment {
    protected val fragment: T;

    constructor(fragment: T, inflater: LayoutInflater, layoutId: Int = 0) : super(inflater.context) {
        if(layoutId > 0)
            inflater.inflate(layoutId, this);
        this.fragment = fragment;
    }

}