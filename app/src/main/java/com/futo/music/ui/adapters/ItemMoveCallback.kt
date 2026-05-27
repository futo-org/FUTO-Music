package com.futo.music.ui.adapters

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.futo.music.constructs.Event1
import com.futo.music.constructs.Event2
import com.futo.music.constructs.Event3

class ItemMoveCallback : ItemTouchHelper.Callback {
    var onRowMoved = Event2<Int, Int>();
    var onRowSelected = Event1<ViewHolder>();
    var onRowClear = Event3<ViewHolder, Int?, Int?>();
    var canEdit = true

    var fromPos: Int? = null;
    var toPos: Int? = null;

    constructor() : super() { }

    override fun isLongPressDragEnabled(): Boolean { return canEdit; }
    override fun isItemViewSwipeEnabled(): Boolean { return false; }

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: ViewHolder): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN;
        return makeMovementFlags(dragFlags, 0);
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: ViewHolder, target: ViewHolder): Boolean {
        if(fromPos == null){
            fromPos = viewHolder.absoluteAdapterPosition;
        }
        toPos = target.absoluteAdapterPosition;

        onRowMoved.emit(viewHolder.absoluteAdapterPosition, target.absoluteAdapterPosition);

        return true;
    }

    override fun onSelectedChanged(viewHolder: ViewHolder?, actionState: Int) {
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
            if (viewHolder != null) {
                onRowSelected.emit(viewHolder);
            }
        }

        super.onSelectedChanged(viewHolder, actionState);
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: ViewHolder) {
        super.clearView(recyclerView, viewHolder);
        val from = fromPos;
        val to = toPos;

        fromPos = null;
        toPos = null;

        onRowClear.emit(viewHolder, from, to);
    }

    override fun onSwiped(viewHolder: ViewHolder, direction: Int) {

    }
}
