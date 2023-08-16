package com.edu.wszib.findyourpet

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.edu.wszib.findyourpet.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso

private lateinit var binding: ActivityMainBinding
lateinit var drawerLayout: DrawerLayout
lateinit var actionBarDrawerToggle: ActionBarDrawerToggle
private lateinit var auth: FirebaseAuth

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        navController.setGraph(R.navigation.nav_graph)
        drawerLayout = findViewById(R.id.drawerLayout)
        actionBarDrawerToggle = ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close)

        // pass the Open and Close toggle for the drawer layout listener
        // to toggle the button
        drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()
        setNavigationDrawerVisibility(true)
        // to make the Navigation drawer icon always appear on the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.navigationView.setNavigationItemSelectedListener(this)
        auth = FirebaseAuth.getInstance()
        val navigationView: NavigationView = findViewById(R.id.navigationView)
        val headerView = navigationView.getHeaderView(0)
        val profileImageView = headerView.findViewById<ImageView>(R.id.profileImageView)
        val usernameTextView = headerView.findViewById<TextView>(R.id.usernameTextView)
        val userName = auth.currentUser?.displayName
        usernameTextView.text = userName
        val userProfileImage = auth.currentUser?.photoUrl
        if(userProfileImage != null)
        {
            Picasso.get()
                .load(userProfileImage)
                .into(profileImageView)
        } else {
            Picasso.get()
                .load(DEFAULT_IMAGE_URL)
                .into(profileImageView)
        }
        profileImageView.setImageURI(auth.currentUser?.photoUrl)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            Log.i("fab",destination.displayName)
            if (destination.id == R.id.mainFragment) {
                binding.fab.isVisible = true
                Log.i("fab","isVisible")
                binding.fab.setOnClickListener {
                    navController.navigate(R.id.chooseFragment)
                }
            } else {
                binding.fab.isVisible = false
                Log.i("fab","isGone")
            }
        }
    }
    fun setNavigationDrawerVisibility(visible: Boolean) {
        if (visible) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            actionBarDrawerToggle.syncState()
        } else {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        }
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        navController.setGraph(R.navigation.nav_graph)

        when (item.itemId) {
            R.id.nav_home -> {
                navController.navigate(R.id.mainFragment)
                drawerLayout.closeDrawers()
                return true
            }
            R.id.nav_logout -> {
                auth = FirebaseAuth.getInstance()
                auth.signOut()
                navController.navigate(R.id.loginFragment)
                drawerLayout.closeDrawers()
                return true
            }
            R.id.nav_settings -> {
                drawerLayout.closeDrawers()
                return true
            }
            R.id.nav_my_lost_found -> {
                navController.navigate(R.id.myLostFoundPetFragment)
                drawerLayout.closeDrawers()
                return true
            }
            // Add more items if needed
        }
        return false
    }
    companion object {
        private const val DEFAULT_IMAGE_URL = "https://i.stack.imgur.com/l60Hf.png"
    }
}