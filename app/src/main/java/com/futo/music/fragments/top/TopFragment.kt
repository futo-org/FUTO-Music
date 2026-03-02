package com.futo.music.fragments.top

import com.futo.music.activities.MainActivity
import com.futo.music.fragments.MainActivityFragment
import com.futo.music.fragments.main.MainFragment

abstract class TopFragment : MainActivityFragment() {

    open fun onShown(parameter: Any? = null) {}
    open fun onHide() {}

    open fun onShowFragment(frag: MainFragment) {
        
    }
    
    fun close() {
        isValidMainActivity();
        return (activity as MainActivity).closeSegment();
    }
}