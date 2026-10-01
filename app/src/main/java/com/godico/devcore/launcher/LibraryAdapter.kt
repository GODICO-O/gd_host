package com.godico.devcore.launcher

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.TextView

data class LibraryItem(
    val fileName: String,
    var isSelected: Boolean = false
)

class LibraryAdapter(
    context: Context,
    val items: MutableList<LibraryItem>
) : ArrayAdapter<LibraryItem>(context, 0, items) {

    var selectedPosition: Int = -1
    val selectedQueue = mutableListOf<LibraryItem>()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_library, parent, false)

        val chkSelected = view.findViewById<CheckBox>(R.id.chkSelected)
        val txtLibraryName = view.findViewById<TextView>(R.id.txtLibraryName)
        val txtOrderIndex = view.findViewById<TextView>(R.id.txtOrderIndex)

        val item = items[position]

        txtLibraryName.text = item.fileName

        // Unbind listener sementara untuk mencegah glitch listener recycle view
        chkSelected.setOnCheckedChangeListener(null)
        chkSelected.isChecked = item.isSelected

        if (position == selectedPosition) {
            view.setBackgroundColor(0x332563EB.toInt())
        } else {
            view.setBackgroundColor(0x00000000)
        }

        val orderIndex = selectedQueue.indexOf(item)
        if (item.isSelected && orderIndex != -1) {
            txtOrderIndex.text = "#${orderIndex + 1}"
            txtOrderIndex.visibility = View.VISIBLE
        } else {
            txtOrderIndex.visibility = View.GONE
        }

        chkSelected.setOnCheckedChangeListener { _, isChecked ->
            item.isSelected = isChecked
            if (isChecked) {
                if (!selectedQueue.contains(item)) {
                    selectedQueue.add(item)
                }
            } else {
                selectedQueue.remove(item)
            }
            notifyDataSetChanged()
        }

        return view
    }

    fun moveUp(position: Int) {
        if (position > 0 && position < items.size) {
            val item = items.removeAt(position)
            items.add(position - 1, item)
            selectedPosition = position - 1
            notifyDataSetChanged()
        }
    }

    fun moveDown(position: Int) {
        if (position >= 0 && position < items.size - 1) {
            val item = items.removeAt(position)
            items.add(position + 1, item)
            selectedPosition = position + 1
            notifyDataSetChanged()
        }
    }

    fun clearQueue() {
        selectedQueue.clear()
        selectedPosition = -1
    }
}
