package com.tikalk.worktracker.time

import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.tikalk.worktracker.R

/**
 * Swipe handler for a row item.
 *
 * @author Moshe Waisberg
 */
internal class TimeListSwipeHandler(private val itemListener: TimeListAdapter.OnTimeListListener) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START) {
    private var deleteBg: Drawable? = null

    override fun isLongPressDragEnabled(): Boolean {
        return false
    }

    override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        if (viewHolder is TimeListViewHolder) {
            val item = viewHolder.record!!
            val id = item.id

            if (id < 0L) {
                return 0
            }
        }
        return super.getSwipeDirs(recyclerView, viewHolder)
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        // We don't want support moving items up/down
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        if (viewHolder is TimeListViewHolder) {
            val item = viewHolder.record!!
            itemListener.onRecordSwipe(item)
        }
    }

    override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

        val itemView = viewHolder.itemView

        // Draw the red delete background
        val right = itemView.right
        val top = itemView.top
        val left = (right + dX).toInt()
        val bottom = itemView.bottom
        if (deleteBg == null) {
            val context = recyclerView.context
            deleteBg = ContextCompat.getDrawable(context, R.drawable.bg_swipe_delete)
        }
        deleteBg!!.setBounds(left, top, right, bottom)
        deleteBg!!.draw(c)
    }
}
