package com.edu.wszib.findyourpet

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.NavHostFragment
import com.edu.wszib.findyourpet.databinding.ActivityMainBinding
import com.firebase.ui.auth.AuthUI
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
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        navController.setGraph(R.navigation.nav_graph)
        drawerLayout = findViewById(R.id.drawerLayout)
        actionBarDrawerToggle =
            ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close)
        showConsentDialogIfNeeded()
        drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()
        setNavigationDrawerVisibility(true)
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
        if (userProfileImage != null) {
            Picasso.get()
                .load(userProfileImage)
                .into(profileImageView)
        } else {
            Picasso.get()
                .load(DEFAULT_IMAGE_URL)
                .into(profileImageView)
        }
        profileImageView.setImageURI(auth.currentUser?.photoUrl)
        val fabHiddenDestinations = setOf(
            R.id.lostCreateFragment,
            R.id.lostEditFragment,
            R.id.foundCreateFragment,
            R.id.foundEditFragment,
            R.id.chooseFragment
        )

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id in fabHiddenDestinations) {
                binding.fab.hide()
            } else {
                binding.fab.show()
            }
        }

        binding.fab.setOnClickListener {
            navController.navigate(R.id.chooseFragment)
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
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        navController.setGraph(R.navigation.nav_graph)

        when (item.itemId) {
            R.id.nav_home -> {
                navController.navigate(R.id.mainFragment)
                drawerLayout.closeDrawers()
                return true
            }

            R.id.nav_logout -> {
                AuthUI.getInstance()
                    .signOut(this)
                    .addOnCompleteListener { navController.navigate(R.id.loginFragment) }
                drawerLayout.closeDrawers()
                return true
            }

            R.id.nav_my_lost_found -> {
                navController.navigate(R.id.myLostFoundPetFragment)
                drawerLayout.closeDrawers()
                return true
            }

            R.id.nav_delete_account -> {
                drawerLayout.closeDrawers()
                deleteAccount()
                return true
            }

            R.id.nav_withdraw_consent -> {
                drawerLayout.closeDrawers()
                withdrawConsent()
                return true
            }
        }
        return false
    }
    private fun deleteAccount() {
        val user = auth.currentUser
        AlertDialog.Builder(this)
            .setTitle("Usuwanie konta")
            .setMessage("Czy na pewno chcesz usunąć swoje konto? Tej operacji nie można cofnąć.")
            .setPositiveButton("Tak, usuń") { _, _ ->
                user?.delete()?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        PrefsManager(this).setConsentGiven(false)
                        Toast.makeText(this, "Konto zostało usunięte", Toast.LENGTH_SHORT).show()

                        val navHostFragment =
                            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                        navHostFragment.navController.navigate(R.id.loginFragment)
                    } else {
                        Toast.makeText(this, "Nie udało się usunąć konta", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    private fun withdrawConsent() {
        AlertDialog.Builder(this)
            .setTitle("Cofnięcie zgody")
            .setMessage("Czy na pewno chcesz cofnąć zgodę na przetwarzanie danych? Aplikacja zostanie zamknięta.")
            .setPositiveButton("Tak, cofam") { _, _ ->
                PrefsManager(this).setConsentGiven(false)
                Toast.makeText(this, "Zgoda została cofnięta", Toast.LENGTH_SHORT).show()
                finishAffinity() 
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }
    private fun showConsentDialogIfNeeded() {
        val prefsManager = PrefsManager(this)
        if (!prefsManager.isConsentGiven()) {
            val message = "Aby korzystać z aplikacji, musisz wyrazić zgodę na przetwarzanie danych osobowych.\n\n" +
                    "Kliknij tutaj, aby przeczytać politykę prywatności."

            val spannableMessage = SpannableString(message)
            val start = message.indexOf("Kliknij tutaj")
            val end = start + "Kliknij tutaj".length

            spannableMessage.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    val intent = Intent(this@MainActivity, PrivacyPolicyActivity::class.java)
                    startActivity(intent)
                }
            }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            val textView = TextView(this).apply {
                text = spannableMessage
                movementMethod = LinkMovementMethod.getInstance()
                setPadding(50, 50, 50, 50)
            }

            AlertDialog.Builder(this)
                .setTitle("Zgoda na przetwarzanie danych")
                .setView(textView)
                .setCancelable(false)
                .setPositiveButton("Zgadzam się") { _, _ ->
                    prefsManager.setConsentGiven()
                }
                .setNegativeButton("Nie zgadzam się") { _, _ ->
                    Toast.makeText(this, "Nie możesz korzystać z aplikacji bez zgody", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .show()
            }
    }

    companion object {
        private const val DEFAULT_IMAGE_URL = "https://i.stack.imgur.com/l60Hf.png"
    }
}