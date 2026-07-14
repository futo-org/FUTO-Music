package com.futo.music.ui.decorator

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.futo.music.dp

class BottomSpacingDecoration: RecyclerView.ItemDecoration {
    private val bottomSpace: Int

    constructor(bottomSpace: Int) {
        this.bottomSpace = bottomSpace;
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)

        if (position == RecyclerView.NO_POSITION)
            return

        if (position == state.itemCount - 1)
            outRect.bottom = bottomSpace
    }
}