package com.edu.wszib.findyourpet

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.edu.wszib.findyourpet.databinding.FragmentLoginBinding
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.BuildConfig
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private var _binding: FragmentLoginBinding? = null
    private val binding: FragmentLoginBinding
        get() = _binding!!

    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { result -> this.onSignInResult(result)}

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth
        binding.signInButton.setOnClickListener { startSignIn() }
        //binding.signOutButton.setOnClickListener { signOut() }

        (requireActivity() as MainActivity).setNavigationDrawerVisibility(false)

    }

    override fun onStart() {
        super.onStart()
        updateUI(auth.currentUser)
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            // Sign in succeeded
            updateUI(auth.currentUser)
        } else {
            // Sign in failed
            Toast.makeText(context, "Sign In Failed", Toast.LENGTH_SHORT).show()
            updateUI(null)
        }
    }

    private fun startSignIn() {
        val intent = AuthUI.getInstance().createSignInIntentBuilder()
            .setIsSmartLockEnabled(!BuildConfig.DEBUG)
            .setAvailableProviders(listOf(AuthUI.IdpConfig.GoogleBuilder().build()))
            .setLogo(R.mipmap.ic_launcher)
            .build()

        signInLauncher.launch(intent)
    }
    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            // Signed in
//            binding.status.text = getString(R.string.firebaseui_status_fmt, user.email)
//            //binding.detail.text = getString(R.string.id_fmt, user.uid)
//
//            //binding.signInButton.visibility = View.GONE
//            //binding.signOutButton.visibility = View.VISIBLE
            findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToMainFragment())
        } else {
            // Signed out
//            binding.status.setText(R.string.signed_out)
//            binding.detail.text = null
//
//            binding.signInButton.visibility = View.VISIBLE
//            binding.signOutButton.visibility = View.GONE
        }
    }

    private fun signOut() {
        AuthUI.getInstance().signOut(requireContext())
        updateUI(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}