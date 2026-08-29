package com.futo.music.ui.views.general

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatSpinner
import com.futo.music.R
import com.futo.music.constructs.Event1

class GeneralDropdown: AppCompatSpinner {

    var availableOptions: Array<String> = arrayOf<String>()
        private set;

    val onSelectedChanged = Event1<Int>();

    constructor(context: Context, attrs: AttributeSet? = null): super(context, attrs) {
        onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selected = availableOptions[position]
                onSelectedChanged.emit(position);
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    fun setOptions(res: Int) {
        val options = resources.getStringArray(res);
        availableOptions = options;
        adapter = SpinnerAdapter(context, availableOptions.toList());
    }
    fun setSelected(index: Int) {
        super.setSelection(index);
    }
}

class SpinnerAdapter(context: Context, private val items: List<String>) : BaseAdapter() {

    override fun getCount() = items.size
    override fun getItem(position: Int) = items[position]
    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(parent.context).inflate(R.layout.general_dropdown_selected, parent, false);
        val text = view.findViewById<TextView>(R.id.text)
        text.text = items[position]
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(parent.context).inflate(R.layout.general_dropdown_item, parent, false)
        val text = view.findViewById<TextView>(R.id.text)
        text.text = items[position]
        return view
    }
}