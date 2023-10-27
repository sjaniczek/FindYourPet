package com.edu.wszib.findyourpet

//import com.edu.wszib.findyourpet.listlostfragments.MyFoundFragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.edu.wszib.findyourpet.databinding.FragmentMyLostFoundPetBinding
import com.edu.wszib.findyourpet.listlostandfoundfragments.MyFoundFragment
import com.edu.wszib.findyourpet.listlostandfoundfragments.MyLostFragment
import com.google.android.material.tabs.TabLayoutMediator

class MyLostFoundPetFragment : Fragment() {

    private lateinit var binding: FragmentMyLostFoundPetBinding
    private lateinit var pagerAdapter: FragmentStateAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMyLostFoundPetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pagerAdapter =
            object : FragmentStateAdapter(childFragmentManager, viewLifecycleOwner.lifecycle) {
                private val fragments = arrayOf<Fragment>(
                    MyLostFragment(),
                    MyFoundFragment(),
                )

                override fun createFragment(position: Int) = fragments[position]

                override fun getItemCount() = fragments.size
            }

        // Set up the ViewPager with the sections adapter.
        with(binding) {
            viewPager.isUserInputEnabled = false
            viewPager.setOnTouchListener { _, _ -> true }
            viewPager.adapter = pagerAdapter
            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.text = when (position) {
                    0 -> "Moje zagubione zwierzęta"
                    else -> "Moje znalezione zwierzęta"
                }
            }.attach()
        }
    }
}

