package org.falaeapp.falae.fragment

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import org.falaeapp.falae.R
import org.falaeapp.falae.databinding.FragmentCreatePageBinding
import org.falaeapp.falae.viewmodel.SpreadsheetViewModel

class CreatePageFragment : Fragment() {

    private var _binding: FragmentCreatePageBinding? = null
    private val sharedViewModel: SpreadsheetViewModel by activityViewModels()

    private lateinit var mPageName: EditText
    private lateinit var mColumnsSize: EditText
    private lateinit var mRowsSize: EditText

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatePageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mPageName = binding.newPageName
        mColumnsSize = binding.columnSize
        mRowsSize = binding.rowSize

        binding.buttonSavePage.setOnClickListener {
            attemptToCreatePage()
        }
        sharedViewModel.createdPageResponse.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandled()?.let { result ->
                result.second?.let { error ->
                    onError(error)
                } ?: run {
                    Toast.makeText(context, "Página criada com sucesso!", Toast.LENGTH_LONG).show()
                    requireActivity().finish()
                }
            }
        }
    }

    private fun onError(error: Exception) {
        Toast.makeText(context, getString(R.string.error_internet_access), Toast.LENGTH_LONG).show()
        error.printStackTrace()
    }

    private fun attemptToCreatePage() {
        mPageName.error = null
        mColumnsSize.error = null
        mRowsSize.error = null

        val pageName = mPageName.text.toString()
        val columnsSize = mColumnsSize.text.toString()
        val rowsSize = mRowsSize.text.toString()

        var cancel = false
        var focusView: View? = null

        if (TextUtils.isEmpty(pageName)) {
            mPageName.error = getString(R.string.error_field_required)
            focusView = mPageName
            cancel = true
        } else if (TextUtils.isEmpty(columnsSize)) {
            mColumnsSize.error = getString(R.string.error_field_required)
            focusView = mColumnsSize
            cancel = true
        } else if (TextUtils.isEmpty(rowsSize)) {
            mRowsSize.error = getString(R.string.error_field_required)
            focusView = mRowsSize
            cancel = true
        }

        if (cancel) {
            focusView?.requestFocus()
        } else {
            sharedViewModel.createPage(pageName, columnsSize.toInt(), rowsSize.toInt())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
