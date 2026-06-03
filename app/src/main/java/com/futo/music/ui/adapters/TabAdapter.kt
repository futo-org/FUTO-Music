package com.futo.music.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout

class TabDescriptor(
    val name: String,
    val fragCreator: ()->Fragment
)

class TabAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle, val tabDescriptors: MutableList<TabDescriptor>) :
    FragmentStateAdapter(fragmentManager, lifecycle) {


    override fun getItemId(position: Int): Long {
        return position.toLong();
    }

    override fun containsItem(itemId: Long): Boolean {
        return itemId >= 0 && itemId < tabDescriptors.size;
    }

    override fun getItemCount(): Int {
        return tabDescriptors.size;
    }

    fun getTabNames(tab: TabLayout.Tab, position: Int) {
        tab.text = tabDescriptors[position].name;
    }

    fun insert(position: Int, tab: TabDescriptor) {
        tabDescriptors.add(position, tab)
        notifyItemInserted(position)
    }

    fun remove(position: Int) {
        tabDescriptors.removeAt(position)
        notifyItemRemoved(position)
    }

    override fun createFragment(position: Int): Fragment {
        val fragment: Fragment
        if(position < 0 || position >= tabDescriptors.size) {
            throw IndexOutOfBoundsException("Tab Position ${position} out of range");
        }

        val descriptor = tabDescriptors[position];
        fragment = descriptor.fragCreator()
        return fragment
    }
}