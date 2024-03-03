package com.example.recycleme

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class NewsAdapter(context: Context, data: ArrayList<String>) : ArrayAdapter<String>(context, 0, data) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var listItemView = convertView
        if (listItemView == null) {
            listItemView = LayoutInflater.from(context).inflate(R.layout.news_list_item, parent, false)
        }

        val itemText = getItem(position)
        val itemTextView = listItemView?.findViewById<TextView>(R.id.itemTextView)
        itemTextView?.text = itemText

        return listItemView!!
    }
}

