package com.edu.wszib.findyourpet.viewholders

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.edu.wszib.findyourpet.R
import com.edu.wszib.findyourpet.models.FoundPetData
import com.squareup.picasso.Picasso

class FoundPetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val listFoundPetAddress: TextView = itemView.findViewById(R.id.listFoundPetAddress)
    private val listFoundPetDate: TextView = itemView.findViewById(R.id.listFoundPetDate)
    private val listFoundPetImg: ImageView = itemView.findViewById(R.id.listFoundPetImg)

    fun bindToLostPet(foundPet: FoundPetData) {
        listFoundPetDate.text = foundPet.foundPetDate
        listFoundPetAddress.text = foundPet.foundPetDecodedAddress

        val lostImageUrl = if (foundPet.foundPetImageUrl.isNullOrEmpty()) {
            DEFAULT_IMAGE_URL
        } else {
            foundPet.foundPetImageUrl
        }
        Picasso.get()
            .load(lostImageUrl)
            .into(listFoundPetImg)
    }
}

private const val DEFAULT_IMAGE_URL = "https://i.stack.imgur.com/l60Hf.png"
