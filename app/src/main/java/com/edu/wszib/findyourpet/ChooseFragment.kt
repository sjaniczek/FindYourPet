package com.edu.wszib.findyourpet

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.edu.wszib.findyourpet.databinding.FragmentChooseBinding


class ChooseFragment : Fragment() {

    private var _binding: FragmentChooseBinding? = null
    private val binding: FragmentChooseBinding
    get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentChooseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonGoToLostPet.setOnClickListener{
            findNavController().navigate(ChooseFragmentDirections.actionChooseFragmentToLostCreateFragment())
        }
        binding.buttonGoToFoundPet.setOnClickListener{
            findNavController().navigate(ChooseFragmentDirections.actionChooseFragmentToLostCreateFragment())
        }
    }
}