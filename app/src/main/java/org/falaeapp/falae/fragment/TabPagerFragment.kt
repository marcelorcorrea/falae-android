package org.falaeapp.falae.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.falaeapp.falae.R
import org.falaeapp.falae.model.SpreadSheet

class TabPagerFragment : Fragment(), SpreadSheetFragment.SpreadSheetFragmentListener {

    private lateinit var mListener: TabPagerFragmentListener
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayoutMediator: TabLayoutMediator

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tab_pager, container, false)
        viewPager = view.findViewById(R.id.viewpager)
        viewPager.adapter = CustomFragmentPagerAdapter(this)
        viewPager.offscreenPageLimit = ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT

        // Give the TabLayout the ViewPager2 using TabLayoutMediator
        val tabLayout = view.findViewById<TabLayout>(R.id.layout_tabs)
        tabLayoutMediator = TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = if (position == 0) {
                resources.getString(R.string.spreadsheets)
            } else {
                resources.getString(R.string.user_info)
            }
        }
        tabLayoutMediator.attach()

        return view
    }

    override fun onDestroyView() {
        tabLayoutMediator.detach()
        super.onDestroyView()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is TabPagerFragmentListener) {
            mListener = context
        } else {
            throw RuntimeException("$context must implement TabPagerFragment")
        }
    }

    override fun displayActivity(spreadSheet: SpreadSheet) {
        mListener.displayActivity(spreadSheet)
    }

    interface TabPagerFragmentListener {
        fun displayActivity(spreadSheet: SpreadSheet)
    }

    /**
     * Adapter moderno para ViewPager2 que substitui o depreciado FragmentStatePagerAdapter
     */
    inner class CustomFragmentPagerAdapter(fragment: Fragment) :
        FragmentStateAdapter(fragment) {

        override fun createFragment(position: Int): Fragment {
            return if (position == 0) {
                SpreadSheetFragment.newInstance()
            } else {
                UserInfoFragment.newInstance()
            }
        }

        override fun getItemCount(): Int = TAB_COUNT
    }

    companion object {
        private const val USER_ID_PARAM = "userIdParam"
        private const val TAB_COUNT = 2
        private const val OFFSCREEN_PAGE_LIMIT = 2

        fun newInstance(): TabPagerFragment {
            return TabPagerFragment()
        }
    }
}
