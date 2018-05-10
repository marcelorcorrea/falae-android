package org.falaeapp.falae.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import org.falaeapp.falae.R
import org.falaeapp.falae.adapter.SpreadSheetAdapter
import org.falaeapp.falae.model.SpreadSheet
import org.falaeapp.falae.model.User
import org.falaeapp.falae.util.Util


class SpreadSheetFragment : Fragment() {

    private lateinit var mListener: SpreadSheetFragmentListener
    private lateinit var spreadSheetAdapter: SpreadSheetAdapter
    private var user: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        user = arguments?.getParcelable(USER_PARAM)
        onAttachFragment(parentFragment)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_spread_sheet, container, false)
        val recyclerView = view.findViewById(R.id.spreadsheet_recycler) as RecyclerView
        user?.let {
            spreadSheetAdapter = SpreadSheetAdapter(context, it.spreadsheets, { spreadSheet ->
                mListener.displayActivity(spreadSheet)
            })
            recyclerView.adapter = spreadSheetAdapter
        }
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
        user?.let {
            if (it.email.contains(EMAIL_SAMPLE).not()) {
                inflater?.inflate(R.menu.board_menu, menu)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.remove_item -> {
                Util.createDialog(
                        context = context,
                        title = getString(R.string.removeUser),
                        message = getString(R.string.questionRemoveUser),
                        positiveText = getString(R.string.yes_option),
                        positiveClick = { mListener.removeUser(user!!) },
                        negativeText = getString(R.string.no_option)
                ).show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    interface SpreadSheetFragmentListener {
        fun displayActivity(spreadSheet: SpreadSheet)
        fun removeUser(user: User)
    }

    companion object {

        private const val USER_PARAM = "userParam"
        private const val EMAIL_SAMPLE = "@falae.com"

        fun newInstance(user: User): SpreadSheetFragment {
            val fragment = SpreadSheetFragment()
            val args = Bundle()
            args.putParcelable(USER_PARAM, user)
            fragment.arguments = args
            return fragment
        }
    }
}
