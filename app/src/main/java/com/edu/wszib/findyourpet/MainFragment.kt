package com.edu.wszib.findyourpet

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.edu.wszib.findyourpet.databinding.FragmentMainBinding
import com.edu.wszib.findyourpet.listlostandfoundfragments.*
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class MainFragment : Fragment() {
    private lateinit var sortMenuItem: MenuItem
    private lateinit var auth: FirebaseAuth
    private var _binding: FragmentMainBinding? = null
    private val binding: FragmentMainBinding
        get() = _binding!!
    private lateinit var pagerAdapter: FragmentStateAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as MainActivity).setNavigationDrawerVisibility(true)

        auth = Firebase.auth
        pagerAdapter =
            object : FragmentStateAdapter(childFragmentManager, viewLifecycleOwner.lifecycle) {
                private val fragments = arrayOf<Fragment>(
                    TopLostFragment(),
                    TopFoundFragment(),
                    PinsMapFragment()
                )

                override fun createFragment(position: Int) = fragments[position]

                override fun getItemCount() = fragments.size
            }

        // Set up the ViewPager with the sections adapter.
        with(binding) {
            container.isUserInputEnabled = false
            container.setOnTouchListener { _, _ -> true }
            container.adapter = pagerAdapter
            TabLayoutMediator(tabs, container) { tab, position ->
                tab.text = when (position) {
                    0 -> getString(R.string.top_lost_pets)
                    1 -> getString(R.string.top_found_pets)
                    else -> "Mapa z pinami"
                }
            }.attach()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}