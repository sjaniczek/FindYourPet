package com.edu.wszib.findyourpet

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.edu.wszib.findyourpet.databinding.FragmentCreateLostBinding

class LostCreateFragment : Fragment() {

    private var _binding: FragmentCreateLostBinding? = null
    private val binding: FragmentCreateLostBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCreateLostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
            binding.buttonGoToMap.setOnClickListener{
                findNavController().navigate(LostCreateFragmentDirections.actionLostCreateFragmentToLostMapsFragment())
            }
    }
}