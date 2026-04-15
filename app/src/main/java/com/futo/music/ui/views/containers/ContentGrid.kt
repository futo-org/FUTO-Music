package com.futo.music.ui.views.containers

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.futo.music.R
import com.futo.music.constructs.Event0
import com.futo.music.constructs.Event1
import com.futo.music.dp
import com.futo.music.models.playable.IPlayable
import com.futo.music.ui.adapters.ContentAdapter
import com.futo.music.ui.buttons.RoundButton
import kotlin.math.floor

class ContentGrid: ConstraintLayout {
    val vertical: Boolean;
    val rowHeight: Int;

    val root: ConstraintLayout;
    val textTitle: TextView;
    val recycler: RecyclerView;

    val buttonList: RoundButton?;

    val adapter: ContentAdapter;

    val onClick = Event1<IPlayable>();
    val onLongClick = Event1<IPlayable>();
    val onOutsideClick = Event0();

    constructor(context: Context, attrs: AttributeSet? = null): super(context, attrs) {
        val attrArr = context.obtainStyledAttributes(attrs, R.styleable.ContentGrid, 0, 0);
        vertical = attrArr.getBoolean(R.styleable.ContentGrid_ContentGrid_vertical, false);
        rowHeight = attrArr.getDimensionPixelSize(R.styleable.ContentGrid_ContentGrid_row_height, 100.dp(resources));

        if(vertical)
            inflate(context, R.layout.view_content_grid_v, this);
        else
            inflate(context, R.layout.view_content_grid_h, this);

        root = findViewById(R.id.root);
        textTitle = findViewById(R.id.text_title);
        recycler = findViewById(R.id.recycler);
        root.setOnClickListener {
            onOutsideClick.emit()
        };
        if(vertical) {
            recycler.layoutManager = GridLayoutManager(context, 3);
            buttonList = null;
        }
        else {
            val layoutManager = LinearLayoutManager(context);
            layoutManager.orientation = LinearLayoutManager.HORIZONTAL;
            recycler.layoutManager = layoutManager;
            buttonList = findViewById(R.id.button_list);
            buttonList.isVisible = false;
        }

        val initialText = attrArr.getString(R.styleable.ContentGrid_ContentGrid_text);
        if(initialText.isNullOrEmpty())
            textTitle.isVisible = false;
        else
            textTitle.text = initialText;

        adapter = ContentAdapter {
            it.setSize(rowHeight, rowHeight);
            it.view.onClick.subscribe {
                onClick.emit(it);
            }
            it.view.onLongClick.subscribe {
                onLongClick.emit(it);
            }
        }
        recycler.adapter = adapter;
    }

    fun setButtonListener(handler: ()->Unit) {
        buttonList?.onClick?.subscribe {
            handler();
        }
        buttonList?.isVisible = true;
    }
    fun setButtonListener(iconRes: Int, handler: ()->Unit) {
        buttonList?.buttonImage?.setImageResource(iconRes);
        buttonList?.onClick?.subscribe {
            handler();
        }
        buttonList?.isVisible = true;
    }

    fun setData(items: List<IPlayable>) {
        adapter.setData(items);
    }

    fun search(query: String) {
        adapter.search(query);
    }
    fun clearSearch(){
        adapter.clearSearch();
    }

    fun getAutoSizeColumns(rowHeight: Int): Int {
        val aspectRatio = 1f;
        val width = rowHeight / aspectRatio;
        val widthTotal = resources.displayMetrics.widthPixels / resources.displayMetrics.density
        val fitWidthCount = floor((widthTotal / width).toDouble());

        return fitWidthCount.toInt();
    }
}