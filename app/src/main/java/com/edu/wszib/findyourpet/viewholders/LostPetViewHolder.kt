package com.edu.wszib.findyourpet.viewholders

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.edu.wszib.findyourpet.R
import com.edu.wszib.findyourpet.models.LostPetData
import com.squareup.picasso.Picasso

class LostPetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val listLostPetName: TextView = itemView.findViewById(R.id.listLostPetName)
    private val listLostPetAddress: TextView = itemView.findViewById(R.id.listLostPetAddress)
    private val listLostPetDate: TextView = itemView.findViewById(R.id.listLostPetDate)
    private val listLostPetDateAdded: TextView = itemView.findViewById(R.id.listLostPetDateAdded)
    private val listLostPetImg: ImageView = itemView.findViewById(R.id.listLostPetImg)

    fun bindToLostPet(lostPet: LostPetData) {
        listLostPetName.text = lostPet.lostPetName
        listLostPetDate.text = buildString {
            append("Zagubiony: ")
            append(lostPet.lostPetDate)
        }
        listLostPetAddress.text = lostPet.lostPetDecodedAddress
        listLostPetDateAdded.text = buildString {
            append("Dodano: ")
            append(lostPet.lostPetDateAdded)
        }
        val lostImageUrl = if (lostPet.lostPetImageUrl.isNullOrEmpty()) {
            DEFAULT_IMAGE_URL
        } else {
            lostPet.lostPetImageUrl
        }
        Picasso.get()
            .load(lostImageUrl)
            .into(listLostPetImg)
    }
}

private const val DEFAULT_IMAGE_URL = "https://i.stack.imgur.com/l60Hf.png"
