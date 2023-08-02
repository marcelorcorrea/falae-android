package org.falaeapp.falae.fragment

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.NavUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import org.falaeapp.falae.R
import org.falaeapp.falae.databinding.FragmentCreateSpreadsheetBinding
import org.falaeapp.falae.viewmodel.SpreadsheetViewModel

class CreateSpreadsheetFragment : Fragment() {
    private var _binding: FragmentCreateSpreadsheetBinding? = null
    private val sharedViewModel: SpreadsheetViewModel by activityViewModels()

    private lateinit var mSpreadsheetName: EditText

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateSpreadsheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mSpreadsheetName = binding.newSpreadsheetName
        binding.saveSpreadsheet.setOnClickListener {
            createSpreadSheet()
        }
        sharedViewModel.createdSpreadsheetResponse.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandled()?.let { result ->
                result.second?.let { error ->
                    onError(error)
                } ?: run {
                    Toast.makeText(context, "Prancha criada com sucesso!", Toast.LENGTH_LONG).show()
                    findNavController().navigate(R.id.action_CreateSpreadsheetFragment_to_CreatePageFragment)
                }
            }
        }
    }

    private fun onError(error: Exception) {
        Toast.makeText(context, getString(R.string.error_internet_access), Toast.LENGTH_LONG).show()
        error.printStackTrace()
    }

    private fun createSpreadSheet() {
        mSpreadsheetName.error = null
        var cancel = false
        var focusView: View? = null
        val name = mSpreadsheetName.text.toString()

        if (TextUtils.isEmpty(name)) {
            mSpreadsheetName.error = getString(R.string.error_field_required)
            focusView = mSpreadsheetName
            cancel = true
        }
        if (cancel) {
            focusView?.requestFocus()
        } else {
            sharedViewModel.createSpreadsheet(mSpreadsheetName.text.toString())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
