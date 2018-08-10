package org.falaeapp.falae.fragment

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.Toast
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
        onAttachFragment(parentFragment)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_spread_sheet, container, false)
        val recyclerView = view.findViewById(R.id.spreadsheet_recycler) as RecyclerView
        userViewModel = ViewModelProviders.of(activity!!).get(UserViewModel::class.java)
        userViewModel.currentUser.observe(activity!!, Observer { user ->
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
        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        recyclerView.layoutManager = layoutManager
        return view
    }

    override fun onAttachFragment(fragment: Fragment?) {
        if (fragment is SpreadSheetFragmentListener) {
            mListener = fragment
        } else {
            throw RuntimeException(fragment!!.toString() + " must implement SpreadSheetFragmentListener")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        userViewModel.currentUser.observe(activity!!, Observer { user ->
            if (user != null && user.email.contains(EMAIL_SAMPLE).not()) {
                inflater?.inflate(R.menu.board_menu, menu)
            }
        })
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

        private const val USER_PARAM = "userParam"
        private const val EMAIL_SAMPLE = "@falaeapp.org"

        fun newInstance(): SpreadSheetFragment {
            return SpreadSheetFragment()
        }
    }
}
