package com.edu.wszib.findyourpet

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.navigation.fragment.NavHostFragment
import com.edu.wszib.findyourpet.databinding.ActivityMainBinding

private lateinit var binding: ActivityMainBinding

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        navController.setGraph(R.navigation.nav_graph)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.mainFragment) {
                binding.fab.isVisible = true
                binding.fab.setOnClickListener {
                    navController.navigate(R.id.action_mainFragment_to_chooseFragment)
                }
            } else {
                binding.fab.isGone = true
            }
        }
    }
}