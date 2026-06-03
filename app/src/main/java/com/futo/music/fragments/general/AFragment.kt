package com.futo.music.fragments.general

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class AFragment(val creator: (inflated: LayoutInflater, container: ViewGroup?)-> View, private val _onCreate: ((context: Context?)->Unit)? = null, private val _onDestroy: (()->Unit)? = null): Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        _onCreate?.invoke(context);
    }

    override fun onDestroy() {
        super.onDestroy()
        _onDestroy?.invoke();
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return creator(inflater, container);
    }

}