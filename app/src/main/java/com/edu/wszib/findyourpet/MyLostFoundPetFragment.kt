package com.edu.wszib.findyourpet

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.edu.wszib.findyourpet.databinding.FragmentMyLostFoundPetBinding
import com.edu.wszib.findyourpet.listlostfragments.MyFoundFragment
//import com.edu.wszib.findyourpet.listlostfragments.MyFoundFragment
import com.edu.wszib.findyourpet.listlostfragments.MyLostFragment
import com.edu.wszib.findyourpet.listlostfragments.TopLostFragment

class MyLostFoundPetFragment : Fragment() {
    private lateinit var binding: FragmentMyLostFoundPetBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMyLostFoundPetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Create instances of LostPetFragment and FoundPetFragment
        val lostPetFragment = MyLostFragment()
        val foundPetFragment = MyFoundFragment()

        // Add the fragments to the layout
        childFragmentManager.beginTransaction()
            .add(binding.containerLayout.id, lostPetFragment)
            .add(binding.containerLayout.id, foundPetFragment)
            .commit()
    }
}
