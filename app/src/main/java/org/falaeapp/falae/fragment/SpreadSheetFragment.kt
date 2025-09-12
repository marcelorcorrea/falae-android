package org.falaeapp.falae.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.falaeapp.falae.R
import org.falaeapp.falae.adapter.SpreadSheetAdapter
import org.falaeapp.falae.model.SpreadSheet
import org.falaeapp.falae.util.Util
import org.falaeapp.falae.viewmodel.UserViewModel

class SpreadSheetFragment : Fragment() {

    private lateinit var mListener: SpreadSheetFragmentListener
    private lateinit var spreadSheetAdapter: SpreadSheetAdapter
    private lateinit var userViewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parentFragment?.let { onAttachFragment(it) }
        // setHasOptionsMenu(true) está depreciado, agora usamos MenuProvider
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_spread_sheet, container, false)
        val recyclerView = view.findViewById(R.id.spreadsheet_recycler) as RecyclerView
        userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)
        userViewModel.currentUser.observe(viewLifecycleOwner, Observer { user ->
            user?.let {
                spreadSheetAdapter = SpreadSheetAdapter(context, it.spreadsheets) { spreadSheet ->
                    spreadSheet.initialPage?.let {
                        mListener.displayActivity(spreadSheet)
                    } ?: run {
                        Toast.makeText(activity, getString(R.string.no_initial_page), Toast.LENGTH_SHORT).show()
                    }
                }
                recyclerView.adapter = spreadSheetAdapter
            }
        })

        // Configurar o MenuProvider (substitui setHasOptionsMenu e onCreateOptionsMenu/onOptionsItemSelected)
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                userViewModel.currentUser.observe(viewLifecycleOwner) { user ->
                    if (user != null && !user.isSampleUser()) {
                        menuInflater.inflate(R.menu.board_menu, menu)
                    }
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
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
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        recyclerView.layoutManager = layoutManager
        return view
    }

    override fun onAttachFragment(fragment: Fragment) {
        if (fragment is SpreadSheetFragmentListener) {
            mListener = fragment
        } else {
            throw RuntimeException("$fragment must implement SpreadSheetFragmentListener")
        }
    }

    // Métodos depreciados substituídos pelo MenuProvider no onCreateView
    // override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) { ... }
    // override fun onOptionsItemSelected(item: MenuItem): Boolean { ... }

    interface SpreadSheetFragmentListener {
        fun displayActivity(spreadSheet: SpreadSheet)
    }

    companion object {

        fun newInstance(): SpreadSheetFragment {
            return SpreadSheetFragment()
        }
    }
}
