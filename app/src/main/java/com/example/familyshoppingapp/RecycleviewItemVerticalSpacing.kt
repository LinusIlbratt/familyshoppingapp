package com.example.familyshoppingapp

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class RecycleviewItemVerticalSpacing(private val verticalSpaceHeight: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {

        if (parent.getChildAdapterPosition(view) != parent.adapter!!.itemCount - 1) {
            outRect.bottom = verticalSpaceHeight
        }
    }
}