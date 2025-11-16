package com.edu.wszib.findyourpet.ui.lostfragments

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.edu.wszib.findyourpet.R
import com.edu.wszib.findyourpet.databinding.FragmentDetailsLostBinding
import com.edu.wszib.findyourpet.models.LostPetViewModel
import com.edu.wszib.findyourpet.models.LostPetViewModelFactory
import com.edu.wszib.findyourpet.repository.LostRepository
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

class LostDetailsFragment : Fragment() {

    private var editMenuItem: MenuItem? = null
    private var deleteMenuItem: MenuItem? = null
    private lateinit var lostPetKey: String
    private var _binding: FragmentDetailsLostBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LostPetViewModel by activityViewModels {
        LostPetViewModelFactory(LostRepository())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailsLostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lostPetKey = requireArguments().getString(EXTRA_POST_KEY)
            ?: throw IllegalArgumentException("Must pass EXTRA_POST_KEY")
        viewModel.loadLostPet(lostPetKey)
        setupMenu()
        observeLostPetData()
        observeReportState()
        observeDeleteState()
        setupReportButton()
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_lost_pet_details, menu)
                editMenuItem = menu.findItem(R.id.action_edit_pet)
                deleteMenuItem = menu.findItem(R.id.action_delete_pet)

                viewModel.editData.value?.let { data ->
                    val isOwner = viewModel.getCurrentUserId() == data.lostPetOwnerId
                    editMenuItem?.isVisible = isOwner
                    deleteMenuItem?.isVisible = isOwner
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_edit_pet -> {
                        navigateToLostPetEdit()
                        true
                    }

                    R.id.action_delete_pet -> {
                        confirmAndDeleteLostPet()
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun observeLostPetData() {
        viewModel.editData.observe(viewLifecycleOwner) { data ->
            data?.let {
                val isOwner = viewModel.getCurrentUserId() == it.lostPetOwnerId
                editMenuItem?.isVisible = isOwner
                deleteMenuItem?.isVisible = isOwner

                binding.apply {
                    val imageUrl =
                        if (it.lostPetImageUrl.isNullOrEmpty()) DEFAULT_IMAGE_URL else it.lostPetImageUrl
                    Picasso.get().load(imageUrl)
                        .placeholder(R.drawable.pets)
                        .error(R.drawable.pets)
                        .into(ivPetImage)
                    tvLostDetailsPetName.text = it.lostPetName
                    tvLostDetailsPetDecodedAddress.text = it.lostPetDecodedAddress
                    tvLostDetailsPetType.text = getString(R.string.details_pet_type, it.lostPetType)
                    tvLostDetailsPetDate.text = getString(R.string.details_pet_date, it.lostPetDate)
                    tvLostDetailsPetHour.text = getString(R.string.details_pet_hour, it.lostPetHour)
                    tvLostDetailsPetBehavior.text =
                        getString(R.string.details_pet_behavior, it.lostPetBehavior)
                    tvLostDetailsPetReact.text =
                        getString(R.string.details_pet_react, it.lostPetReact)
                    tvLostDetailsPetAdditionalInfo.text =
                        getString(R.string.details_pet_additional, it.lostPetAdditionalPetInfo)
                    tvLostDetailsPetOwnerName.text =
                        getString(R.string.details_pet_owner_name, it.lostPetOwnerName)
                    tvLostDetailsPetPhoneNumber.text =
                        getString(R.string.details_pet_owner_number, it.lostPetPhoneNumber)
                    tvLostDetailsPetEmailAddress.text =
                        getString(R.string.details_pet_owner_email, it.lostPetEmailAddress)
                    tvLostDetailsPetOwnerAdditionalInfo.text = getString(
                        R.string.details_pet_owner_additional,
                        it.lostPetAdditionalOwnerInfo
                    )

                    lostDetailsMapButton.setOnClickListener { loc ->
                        it.lostPetLocation?.let { loc ->
                            val uri = Uri.parse("geo:0,0?q=${loc.latitude},${loc.longitude}")
                            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            startActivity(mapIntent)
                        }
                    }

                    lostPetPhoneButton.setOnClickListener {
                        val dialIntent =
                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:${data.lostPetPhoneNumber}"))
                        startActivity(dialIntent)
                    }

                    lostDetailsSmsButton.setOnClickListener {
                        val smsUri = Uri.parse("smsto:${data.lostPetPhoneNumber}")
                        val smsIntent = Intent(Intent.ACTION_SENDTO, smsUri)
                        smsIntent.putExtra(
                            "sms_body",
                            "Dzień dobry, kontaktuję się w sprawie odnalezionego zwierzaka."
                        )
                        startActivity(smsIntent)
                    }
                }
            }
        }
    }

    private fun observeReportState() {
        lifecycleScope.launch {
            viewModel.reportState.collect { result ->
                result?.let {
                    if (it.isSuccess) {
                        Toast.makeText(requireContext(), "Zgłoszenie wysłane", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Błąd podczas wysyłania zgłoszenia",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    viewModel.resetReportState()
                }
            }
        }
    }

    private fun setupReportButton() {
        binding.buttonReportLost.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
                .setTitle("Zgłoś ogłoszenie")

            val input = EditText(requireContext()).apply { hint = "Wpisz powód zgłoszenia" }
            builder.setView(input)

            builder.setPositiveButton("Wyślij") { dialog, _ ->
                val message = input.text.toString().trim()
                if (message.isNotEmpty()) {
                    viewModel.sendReport(lostPetKey, message)

                } else {
                    Toast.makeText(
                        requireContext(),
                        "Treść zgłoszenia nie może być pusta",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                dialog.dismiss()
            }

            builder.setNegativeButton("Anuluj") { dialog, _ -> dialog.cancel() }

            builder.show()
        }
    }

    private fun observeDeleteState() {
        lifecycleScope.launch {
            viewModel.deleteState.collect { result ->
                result ?: return@collect

                if (result.isSuccess) {
                    Toast.makeText(requireContext(), "Usunięto ogłoszenie", Toast.LENGTH_SHORT)
                        .show()
                    findNavController().navigate(R.id.mainFragment)
                } else {
                    Toast.makeText(requireContext(), "Błąd podczas usuwania", Toast.LENGTH_SHORT)
                        .show()
                }

                viewModel.resetDeleteState()
            }
        }
    }

    private fun confirmAndDeleteLostPet() {
        viewModel.getCurrentUserId() ?: return
        val builder = AlertDialog.Builder(requireContext())
            .setTitle("Usuń ogłoszenie")
            .setMessage("Czy jesteś pewny, że chcesz usunąć to ogłoszenie?")
            .setPositiveButton("Tak") { _, _ ->
                viewModel.deleteLostPet(lostPetKey)
            }
            .setNegativeButton("Anuluj", null)
        builder.show()
    }

    private fun navigateToLostPetEdit() {
        val args = bundleOf(LostEditFragment.LOST_EDIT_POST_KEY to lostPetKey)
        findNavController().navigate(R.id.lostEditFragment, args)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val DEFAULT_IMAGE_URL = "https://i.stack.imgur.com/l60Hf.png"
        const val EXTRA_POST_KEY = "post_key"
    }
}