package com.edu.wszib.findyourpet.ui.foundfragments

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.edu.wszib.findyourpet.R
import com.edu.wszib.findyourpet.databinding.FragmentFoundDetailsBinding
import com.edu.wszib.findyourpet.models.FoundPetViewModel
import com.edu.wszib.findyourpet.models.FoundPetViewModelFactory
import com.edu.wszib.findyourpet.repository.FoundRepository
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

class FoundDetailsFragment : Fragment() {

    private var _binding: FragmentFoundDetailsBinding? = null
    private val binding get() = _binding!!

    private lateinit var foundPetKey: String
    private var editMenuItem: MenuItem? = null
    private var deleteMenuItem: MenuItem? = null

    private val viewModel: FoundPetViewModel by activityViewModels {
        FoundPetViewModelFactory(FoundRepository())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFoundDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        foundPetKey = requireArguments().getString(EXTRA_POST_KEY)
            ?: throw IllegalArgumentException("Must pass EXTRA_POST_KEY")
        viewModel.loadFoundPet(foundPetKey)
        setupMenu()
        observeFoundPetData()
        observeReportState()
        observeDeleteState()
        setupReportButton()
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_found_pet_details, menu)
                editMenuItem = menu.findItem(R.id.action_edit_pet)
                deleteMenuItem = menu.findItem(R.id.action_delete_pet)

                viewModel.editData.value?.let { data ->
                    val isOwner = viewModel.getCurrentUserId() == data.foundPetOwnerId
                    editMenuItem?.isVisible = isOwner
                    deleteMenuItem?.isVisible = isOwner
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_edit_pet -> {
                        navigateToFoundPetEdit()
                        true
                    }
                    R.id.action_delete_pet -> {
                        confirmAndDeleteFoundPet()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun observeFoundPetData() {
        viewModel.editData.observe(viewLifecycleOwner, Observer { data ->
            data?.let {
                val isOwner = viewModel.getCurrentUserId() == it.foundPetOwnerId
                editMenuItem?.isVisible = isOwner
                deleteMenuItem?.isVisible = isOwner

                binding.apply {
                    val imageUrl = if (it.foundPetImageUrl.isNullOrEmpty()) DEFAULT_IMAGE_URL else it.foundPetImageUrl
                    Picasso.get().load(imageUrl)
                        .placeholder(R.drawable.pets)
                        .error(R.drawable.pets)
                        .into(ivPetImage)

                    tvFoundDetailsPetDecodedAddress.text = it.foundPetDecodedAddress
                    tvFoundDetailsPetType.text = getString(R.string.details_found_pet_type, it.foundPetType)
                    tvFoundDetailsPetDate.text = getString(R.string.details_found_pet_date, it.foundPetDate)
                    tvFoundDetailsPetBehavior.text = getString(R.string.details_pet_behavior, it.foundPetBehavior)
                    tvFoundDetailsPetAdditionalInfo.text = getString(R.string.details_pet_additional, it.foundPetAdditionalPetInfo)
                    tvFoundDetailsPetFinderName.text = getString(R.string.details_pet_finder_name, it.foundPetFinderName)
                    tvFoundDetailsPetPhoneNumber.text = getString(R.string.details_pet_finder_number, it.foundPetPhoneNumber)
                    tvFoundDetailsPetEmailAddress.text = getString(R.string.details_pet_finder_email, it.foundPetEmailAddress)
                    tvFoundDetailsPetOwnerAdditionalInfo.text = getString(R.string.details_pet_finder_additional, it.foundPetAdditionalFinderInfo)

                    foundDetailsMapButton.setOnClickListener { loc ->
                        it.foundPetLocation?.let { loc ->
                            val uri = Uri.parse("geo:0,0?q=${loc.latitude},${loc.longitude}")
                            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            startActivity(mapIntent)
                        }
                    }

                    foundPetPhoneButton.setOnClickListener {
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${data.foundPetPhoneNumber}"))
                        startActivity(dialIntent)
                    }

                    foundDetailsSmsButton.setOnClickListener {
                        val smsUri = Uri.parse("smsto:${data.foundPetPhoneNumber}")
                        val smsIntent = Intent(Intent.ACTION_SENDTO, smsUri)
                        smsIntent.putExtra("sms_body", "Dzień dobry, kontaktuję się w sprawie odnalezionego zwierzaka.")
                        startActivity(smsIntent)
                    }
                }
            }
        })
    }
    private fun observeReportState() {
        lifecycleScope.launch {
            viewModel.reportState.collect { result ->
                result?.let {
                    if (it.isSuccess) {
                        Toast.makeText(requireContext(), "Zgłoszenie wysłane", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Błąd podczas wysyłania zgłoszenia", Toast.LENGTH_SHORT).show()
                    }

                    viewModel.resetReportState()
                }
            }
        }
    }
    private fun setupReportButton() {
        binding.buttonReportFound.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
                .setTitle("Zgłoś ogłoszenie")

            val input = EditText(requireContext()).apply { hint = "Wpisz powód zgłoszenia" }
            builder.setView(input)

            builder.setPositiveButton("Wyślij") { dialog, _ ->
                val message = input.text.toString().trim()
                if (message.isNotEmpty()) {
                    viewModel.sendReport(foundPetKey, message)

                } else {
                    Toast.makeText(requireContext(), "Treść zgłoszenia nie może być pusta", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(requireContext(), "Usunięto ogłoszenie", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.mainFragment)
                } else {
                    Toast.makeText(requireContext(), "Błąd podczas usuwania", Toast.LENGTH_SHORT).show()
                }

                viewModel.resetDeleteState()
            }
        }
    }
    private fun confirmAndDeleteFoundPet() {
        viewModel.getCurrentUserId() ?: return
        val builder = AlertDialog.Builder(requireContext())
            .setTitle("Usuń ogłoszenie")
            .setMessage("Czy jesteś pewny, że chcesz usunąć to ogłoszenie?")
            .setPositiveButton("Tak") { _, _ ->
                viewModel.deleteFoundPet(foundPetKey)
            }
            .setNegativeButton("Anuluj", null)
        builder.show()
    }

    private fun navigateToFoundPetEdit() {
        val args = bundleOf(FoundEditFragment.FOUND_EDIT_POST_KEY to foundPetKey)
        findNavController().navigate(R.id.foundEditFragment, args)
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
