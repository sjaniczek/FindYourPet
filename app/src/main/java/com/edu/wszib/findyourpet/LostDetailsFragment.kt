package com.edu.wszib.findyourpet

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.edu.wszib.findyourpet.databinding.FragmentDetailsLostBinding

class LostDetailsFragment : Fragment() {

    private var _binding: FragmentDetailsLostBinding? = null
    private val binding: FragmentDetailsLostBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDetailsLostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
    companion object {
        private const val TAG = "PostDetailFragment"
        const val EXTRA_POST_KEY = "post_key"
    }
}