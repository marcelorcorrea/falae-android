package org.falaeapp.falae.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.falaeapp.falae.R
import org.falaeapp.falae.adapter.SpreadSheetAdapter
import org.falaeapp.falae.databinding.FragmentSpreadSheetBinding
import org.falaeapp.falae.model.SpreadSheet
import org.falaeapp.falae.util.Util
import org.falaeapp.falae.viewmodel.UserViewModel

class SpreadSheetFragment : Fragment() {

    private lateinit var mListener: SpreadSheetFragmentListener
    private lateinit var spreadSheetAdapter: SpreadSheetAdapter
    private lateinit var userViewModel: UserViewModel
    private var _binding: FragmentSpreadSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onAttachFragment(parentFragment!!)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSpreadSheetBinding.inflate(inflater, container, false)
        val view = binding.root
        val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(activity!!.application)
        userViewModel = ViewModelProvider(activity!!, factory)[UserViewModel::class.java]
        userViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user?.let {
                spreadSheetAdapter = SpreadSheetAdapter(context, it.spreadsheets) { spreadSheet ->
                    spreadSheet.initialPage?.let {
                        mListener.displayActivity(spreadSheet)
                    } ?: run {
                        Toast.makeText(activity, getString(R.string.no_initial_page), Toast.LENGTH_SHORT).show()
                    }
                }
                binding.spreadsheetRecycler.adapter = spreadSheetAdapter
            }
        }
        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        binding.spreadsheetRecycler.layoutManager = layoutManager
        return view
    }

    override fun onAttachFragment(fragment: Fragment) {
        if (fragment is SpreadSheetFragmentListener) {
            mListener = fragment
        } else {
            throw RuntimeException("$fragment must implement SpreadSheetFragmentListener")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        userViewModel.currentUser.observe(
            viewLifecycleOwner
        ) { user ->
            if (user != null && !user.isSampleUser()) {
                inflater.inflate(R.menu.board_menu, menu)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.remove_item -> {
                context?.let { context ->
                    Util.createDialog(
                        context = context,
                        title = getString(R.string.removeUser),
                        message = getString(R.string.questionRemoveUser),
                        positiveText = getString(R.string.yes_option),
                        positiveClick = { userViewModel.removeUser() },
                        negativeText = getString(R.string.no_option)
                    ).show()
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    interface SpreadSheetFragmentListener {
        fun displayActivity(spreadSheet: SpreadSheet)
    }

    companion object {

        fun newInstance(): SpreadSheetFragment {
            return SpreadSheetFragment()
        }
    }
}
