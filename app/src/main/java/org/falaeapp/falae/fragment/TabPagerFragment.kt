package org.falaeapp.falae.fragment

import android.content.Context
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.falaeapp.falae.R
import org.falaeapp.falae.model.SpreadSheet
import org.falaeapp.falae.model.User

class TabPagerFragment : Fragment(), SpreadSheetFragment.SpreadSheetFragmentListener {

    private lateinit var user: User
    private lateinit var mListener: TabPagerFragmentListener
    private lateinit var viewPager: ViewPager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        user = arguments?.getParcelable(USER_PARAM) ?: return
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_tab_pager, container, false)
        viewPager = view.findViewById(R.id.viewpager) as ViewPager
        viewPager.adapter = CustomFragmentPagerAdapter(childFragmentManager, context)
        viewPager.offscreenPageLimit = OFFSCREEN_PAGE_LIMIT

        // Give the TabLayout the ViewPager
        val tabLayout = view.findViewById(R.id.layout_tabs) as TabLayout
        tabLayout.setupWithViewPager(viewPager)
        return view
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is TabPagerFragmentListener) {
            mListener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement TabPagerFragment")
        }
    }

    override fun displayActivity(spreadSheet: SpreadSheet) {
        mListener.displayActivity(spreadSheet)
    }

    override fun removeUser(user: User) {
        mListener.removeUser(user)
    }

    interface TabPagerFragmentListener {
        fun displayActivity(spreadSheet: SpreadSheet)
        fun removeUser(user: User)
    }

    inner class CustomFragmentPagerAdapter(fm: FragmentManager, private val context: Context?) : FragmentStatePagerAdapter(fm) {
        private val tabTitles = arrayOf(resources.getString(R.string.spreadsheets), resources.getString(R.string.user_info))

        override fun getItem(position: Int): Fragment {
            return if (position == 0) {
                SpreadSheetFragment.newInstance(user!!)
            } else {
                UserInfoFragment.newInstance(user!!)
            }
        }

        override fun getPageTitle(position: Int): CharSequence = tabTitles[position]

        override fun getCount(): Int = TAB_COUNT
    }

    companion object {

        private const val USER_PARAM = "userParam"
        private const val TAB_COUNT = 2
        private const val OFFSCREEN_PAGE_LIMIT = 2

        fun newInstance(user: User?): TabPagerFragment {
            val fragment = TabPagerFragment()
            val args = Bundle()
            args.putParcelable(USER_PARAM, user)
            fragment.arguments = args
            return fragment
        }
    }
}
