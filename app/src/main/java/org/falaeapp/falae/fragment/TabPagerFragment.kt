package org.falaeapp.falae.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import org.falaeapp.falae.R
import org.falaeapp.falae.databinding.FragmentTabPagerBinding
import org.falaeapp.falae.model.SpreadSheet

class TabPagerFragment : Fragment(), SpreadSheetFragment.SpreadSheetFragmentListener {

    private lateinit var mListener: TabPagerFragmentListener
    private lateinit var viewPager: ViewPager
    private var _binding: FragmentTabPagerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTabPagerBinding.inflate(inflater, container, false)
        val view = binding.root
        viewPager = binding.viewpager
        viewPager.adapter = CustomFragmentPagerAdapter(childFragmentManager, context)
        viewPager.offscreenPageLimit = OFFSCREEN_PAGE_LIMIT

        // Give the TabLayout the ViewPager
        binding.layoutTabs.setupWithViewPager(viewPager)
        return view
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

    inner class CustomFragmentPagerAdapter(fm: FragmentManager, private val context: Context?) :
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        private val tabTitles =
            arrayOf(resources.getString(R.string.spreadsheets), resources.getString(R.string.user_info))

        override fun getItem(position: Int): Fragment {
            return if (position == 0) {
                SpreadSheetFragment.newInstance()
            } else {
                UserInfoFragment.newInstance()
            }
        }

        override fun getPageTitle(position: Int): CharSequence = tabTitles[position]

        override fun getCount(): Int = TAB_COUNT
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
