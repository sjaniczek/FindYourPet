package com.edu.wszib.findyourpet.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.edu.wszib.findyourpet.databinding.FragmentChooseBinding
import com.edu.wszib.findyourpet.models.FoundPetViewModel
import com.edu.wszib.findyourpet.models.FoundPetViewModelFactory
import com.edu.wszib.findyourpet.models.LostPetViewModel
import com.edu.wszib.findyourpet.models.LostPetViewModelFactory
import com.edu.wszib.findyourpet.repository.FoundRepository
import com.edu.wszib.findyourpet.repository.LostRepository
import com.google.firebase.auth.FirebaseAuth


class ChooseFragment : Fragment() {

    private var _binding: FragmentChooseBinding? = null
    private val binding: FragmentChooseBinding
        get() = _binding!!
    private val foundViewModel: FoundPetViewModel by activityViewModels {
        FoundPetViewModelFactory(FoundRepository())
    }
    private val lostViewModel: LostPetViewModel by activityViewModels {
        LostPetViewModelFactory(LostRepository())
    }

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
        val user = FirebaseAuth.getInstance().currentUser
        binding.buttonGoToLostPet.setOnClickListener {
            if (user != null) {
                lostViewModel.clearData()
                lostViewModel.resetEditData()
                lostViewModel.resetAllStates()
                findNavController().navigate(ChooseFragmentDirections.actionChooseFragmentToLostCreateFragment())
            } else {
                showDialog()
            }
        }
        binding.buttonGoToFoundPet.setOnClickListener {
            if (user != null) {
                foundViewModel.clearData()
                foundViewModel.resetEditData()
                foundViewModel.resetAllStates()
                findNavController().navigate(ChooseFragmentDirections.actionChooseFragmentToFoundCreateFragment())
            } else {
                showDialog()
            }
        }
    }

    private fun showDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
            .setMessage("Aby dodać swój post z zagubionym/odnalezionym zwierzęciem musisz się zalogować!")
            .setTitle("Użytkownik nie zalogowany")
            .setPositiveButton("Przejdź do logowania") { dialog, which ->
                findNavController().navigate(ChooseFragmentDirections.actionChooseFragmentToLoginFragment())

            }
            .setNegativeButton("Anuluj") { dialog, which ->
                findNavController().navigateUp()
            }

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }
}