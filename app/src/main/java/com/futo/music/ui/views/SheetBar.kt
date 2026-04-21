package com.futo.music.ui.views

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import com.futo.music.R

class SheetBar: LinearLayout {

    constructor(context: Context) : super(context) {
        inflate(context, R.layout.view_sheet_bar, this);
    }
}