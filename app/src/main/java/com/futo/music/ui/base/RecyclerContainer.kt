package com.futo.music.ui.base

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.futo.music.R


open class RecyclerContainer: ConstraintLayout {

    protected val _containerBefore: LinearLayout;
    protected val _containerAfter: LinearLayout;
    protected val _recycler: RecyclerView;


    constructor(context: Context) : super(context) {
        inflate(context, R.layout.fragment_recycler, this);

        _containerBefore = findViewById(R.id.container_before);
        _containerAfter = findViewById(R.id.container_after);
        _recycler = findViewById(R.id.recycler);
    }

    fun setTopViews(views: List<View>) {
        _containerBefore.removeAllViews();
        for(view in views){
            _containerBefore.addView(view);
        }
    }
    fun setBottomViews(views: List<View>) {
        _containerAfter.removeAllViews();
        for(view in views){
            _containerAfter.addView(view);
        }
    }
}