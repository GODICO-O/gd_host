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
    private val items: MutableList<LibraryItem>
) : ArrayAdapter<LibraryItem>(context, 0, items) {

    var selectedPosition: Int = -1
    // Antrean urutan berdasarkan klik pengguna
    val selectedQueue = mutableListOf<LibraryItem>()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_library, parent, false)

        val chkSelected = view.findViewById<CheckBox>(R.id.chkSelected)
        val txtLibraryName = view.findViewById<TextView>(R.id.txtLibraryName)
        val txtOrderIndex = view.findViewById<TextView>(R.id.txtOrderIndex)

        val item = items[position]

        txtLibraryName.text = item.fileName
        chkSelected.isChecked = item.isSelected

        // Visual Selection untuk Move Up/Down
        if (position == selectedPosition) {
            view.setBackgroundColor(0x332563EB.toInt())
        } else {
            view.setBackgroundColor(0x00000000)
        }

        // Tampilkan nomor urut berdasarkan posisi di selectedQueue
        val orderIndex = selectedQueue.indexOf(item)
        if (item.isSelected && orderIndex != -1) {
            txtOrderIndex.text = "#${orderIndex + 1}"
            txtOrderIndex.visibility = View.VISIBLE
        } else {
            txtOrderIndex.visibility = View.GONE
        }

        chkSelected.setOnClickListener {
            val isChecked = chkSelected.isChecked
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
            val temp = items[position]
            items[position] = items[position - 1]
            items[position - 1] = temp
            selectedPosition = position - 1
            notifyDataSetChanged()
        }
    }

    fun moveDown(position: Int) {
        if (position >= 0 && position < items.size - 1) {
            val temp = items[position]
            items[position] = items[position + 1]
            items[position + 1] = temp
            selectedPosition = position + 1
            notifyDataSetChanged()
        }
    }

    fun clearQueue() {
        selectedQueue.clear()
        selectedPosition = -1
    }
}
