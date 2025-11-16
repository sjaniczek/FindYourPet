package com.edu.wszib.findyourpet.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.edu.wszib.findyourpet.R
import com.edu.wszib.findyourpet.models.LostPetData
import com.edu.wszib.findyourpet.viewholders.LostPetViewHolder

class LostPetListAdapter(
    private val items: List<LostPetData>,
    private val onItemClick: (LostPetData) -> Unit
) : RecyclerView.Adapter<LostPetViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LostPetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.lost_list_iem, parent, false)
        return LostPetViewHolder(view)
    }

    override fun onBindViewHolder(holder: LostPetViewHolder, position: Int) {
        val item = items[position]

        // Bind LostPetData to ViewHolder UI
        holder.bindToLostPet(item)

        // Handle click event on the specific list item
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = items.size // Total number of lost pet items
}
