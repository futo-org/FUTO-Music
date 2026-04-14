package com.futo.music.ui.views.grid

import androidx.constraintlayout.widget.ConstraintLayout
import com.futo.music.constructs.Event1
import com.futo.music.models.playable.IPlayable

interface IContentGridView {
    val root: ConstraintLayout;

    val onClick: Event1<IPlayable>
    val onLongClick: Event1<IPlayable>

    fun bind(playable: IPlayable);

    fun setSize(width: Int, height: Int);
}