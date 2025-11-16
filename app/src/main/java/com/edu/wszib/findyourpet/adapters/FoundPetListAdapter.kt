package com.edu.wszib.findyourpet.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.edu.wszib.findyourpet.R
import com.edu.wszib.findyourpet.models.FoundPetData
import com.edu.wszib.findyourpet.viewholders.FoundPetViewHolder

class FoundPetListAdapter(
    private val items: List<FoundPetData>,
    private val onItemClick: (FoundPetData) -> Unit
) : RecyclerView.Adapter<FoundPetViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoundPetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.found_list_item, parent, false)
        return FoundPetViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoundPetViewHolder, position: Int) {
        val item = items[position]

        // Bind FoundPetData to ViewHolder UI
        holder.bindToFoundPet(item)

        // Handle click event on the specific list item
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = items.size // Number of items displayed in the list
}
