package com.example.familyshoppingapp

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class MyDiscoveriesAdapter(private var myPlaces: List<MyPlace>, private val onItemClicked: (MyPlace) -> Unit) :
    RecyclerView.Adapter<MyDiscoveriesAdapter.ViewHolder>() {

    class ViewHolder(view: View, private val onItemClicked: (MyPlace) -> Unit) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.text_view_search_result)

        fun bind(myPlace: MyPlace) {
            textView.text = myPlace.name

            itemView.setOnClickListener {
                Log.d("!!!", "Clicked on: ${myPlace.name}")
                onItemClicked(myPlace)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.my_discoveries_items, parent, false)
        return ViewHolder(view, onItemClicked)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val hiddenGem = myPlaces[position]
        holder.bind(hiddenGem)
    }

    override fun getItemCount() = myPlaces.size

    fun updateData(newMyPlaces: List<MyPlace>) {
        myPlaces = newMyPlaces
        notifyDataSetChanged()
    }
}
