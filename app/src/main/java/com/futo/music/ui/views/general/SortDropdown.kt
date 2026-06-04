package com.futo.music.ui.views.general

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.SpinnerAdapter
import android.widget.TextView
import com.futo.music.R
import com.futo.music.constructs.Event1

enum class SortDropdownType {
    Alphabetic,
    AlphabeticDesc,
    Added,
    AddedDesc,
    Played,
    PlayedDesc,
    Count,
    CountDesc
}

class SortDropdown: androidx.appcompat.widget.AppCompatSpinner {

    companion object {
        val OPTIONS = listOf(SortDropdownType.Alphabetic,
            SortDropdownType.AlphabeticDesc,
            SortDropdownType.Added,
            SortDropdownType.AddedDesc,
            SortDropdownType.Played,
            SortDropdownType.PlayedDesc,
            SortDropdownType.Count,
            SortDropdownType.CountDesc);
    }

    var availableOptions: List<SortDropdownType> = OPTIONS
        private set;

    val onSelectedChanged = Event1<SortDropdownType>();

    constructor(context: Context, attrs: AttributeSet? = null): super(context, attrs) {
        adapter = getDropdownOptions(OPTIONS);

        onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selected = availableOptions[position]
                onSelectedChanged.emit(selected);
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    fun setFilters(categories: List<SortDropdownType>) {
        adapter = getDropdownOptions(categories);
        availableOptions = categories;
    }

    fun setSelected(type: SortDropdownType) {
        super.setSelection(availableOptions.indexOf(type));
    }

    private fun getDropdownOptions(cats: List<SortDropdownType>): IconSpinnerAdapter {

        val items = mutableListOf<SpinnerItem>();

        for(option in cats) {
            val result = when(option) {
                SortDropdownType.Alphabetic -> SpinnerItem(R.drawable.ic_alphabetic, "Alphabetic (Asc)");
                SortDropdownType.AlphabeticDesc -> SpinnerItem(R.drawable.ic_alphabetic, "Alphabetic (Desc)");
                SortDropdownType.Added -> SpinnerItem(R.drawable.ic_date, "Added (Oldest)");
                SortDropdownType.AddedDesc -> SpinnerItem(R.drawable.ic_date, "Added (Newest)");
                SortDropdownType.Played -> SpinnerItem(R.drawable.ic_played, "Played (Oldest)");
                SortDropdownType.PlayedDesc -> SpinnerItem(R.drawable.ic_played, "Played (Newest)");
                SortDropdownType.Count -> SpinnerItem(R.drawable.ic_count, "Count (Asc)");
                SortDropdownType.CountDesc -> SpinnerItem(R.drawable.ic_count, "Count (Desc)");
                else -> null
            }
            if(result != null)
                items.add(result);
        }
        return IconSpinnerAdapter(context, items);
    }
}
data class SpinnerItem(val iconRes: Int, val text: String)
class IconSpinnerAdapter(context: Context, private val items: List<SpinnerItem>) : BaseAdapter() {

    override fun getCount() = items.size
    override fun getItem(position: Int) = items[position]
    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(parent.context).inflate(R.layout.sort_dropdown_selected, parent, false)
        val icon = view.findViewById<ImageView>(R.id.icon)
        icon.setImageResource(items[position].iconRes)
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(parent.context).inflate(R.layout.sort_dropdown_item, parent, false)
        val icon = view.findViewById<ImageView>(R.id.icon)
        val text = view.findViewById<TextView>(R.id.text)
        icon.setImageResource(items[position].iconRes)
        text.text = items[position].text
        return view
    }
}