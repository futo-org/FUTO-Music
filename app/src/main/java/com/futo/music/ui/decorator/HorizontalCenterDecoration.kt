package com.futo.music.ui.decorator

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class HorizontalCenterDecoration : RecyclerView.ItemDecoration {
    val spaceWidth: Int;


    constructor(spaceWidth: Int) {
        this.spaceWidth = spaceWidth;
    }


    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val itemWidth = view.layoutParams.width

        val offset = (spaceWidth - itemWidth) / 2
        outRect.left = offset
        outRect.right = offset
    }
}