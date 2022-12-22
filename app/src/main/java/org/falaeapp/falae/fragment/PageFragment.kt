package org.falaeapp.falae.fragment

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import org.falaeapp.falae.R
import org.falaeapp.falae.adapter.ItemPagerAdapter
import org.falaeapp.falae.databinding.FragmentPageBinding
import org.falaeapp.falae.viewmodel.DisplayViewModel

class PageFragment : Fragment(), ViewPagerItemFragment.PageInteractionListener {

    private lateinit var mPageFragmentListener: PageFragmentListener
    private lateinit var mPager: ViewPager
    private lateinit var mPagerAdapter: ItemPagerAdapter
    private lateinit var leftNav: ImageView
    private lateinit var rightNav: ImageView
    private lateinit var leftNavHolder: FrameLayout
    private lateinit var rightNavHolder: FrameLayout
    private lateinit var displayViewModel: DisplayViewModel
    private var _binding: FragmentPageBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(activity!!.application)
        displayViewModel = ViewModelProvider(activity!!, factory)[DisplayViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPageBinding.inflate(inflater, container, false)
        val view = binding.root
        leftNav = binding.leftNav
        rightNav = binding.rightNav
        leftNavHolder = binding.leftNavHolder
        rightNavHolder = binding.rightNavHolder

        val vto = view.viewTreeObserver
        vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    view.viewTreeObserver.removeGlobalOnLayoutListener(this)
                } else {
                    view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
                mPager = binding.pager
                val navHoldersSize = java.lang.Double.valueOf(mPager.measuredWidth * 0.065).toInt()
                leftNav.layoutParams.width = navHoldersSize
                leftNav.layoutParams.height = navHoldersSize
                rightNav.layoutParams.width = navHoldersSize
                rightNav.layoutParams.height = navHoldersSize
                leftNavHolder.layoutParams.width = navHoldersSize
                rightNavHolder.layoutParams.width = navHoldersSize
                if (isPagerAdapterInitialized().not()) {
                    displayViewModel.currentPage.observe(
                        this@PageFragment
                    ) { page ->
                        mPagerAdapter = ItemPagerAdapter(childFragmentManager, page, navHoldersSize * 2)
                    }
                }
                if (::mPager.isInitialized && isPagerAdapterInitialized()) {
                    mPager.adapter = mPagerAdapter
                }
                val pagerLayoutParams = mPager.layoutParams as ViewGroup.MarginLayoutParams
                pagerLayoutParams.leftMargin += navHoldersSize
                pagerLayoutParams.rightMargin += navHoldersSize
                mPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
                    override fun onPageSelected(position: Int) {
                        handleNavButtons()
                    }
                })
                if (shouldEnableNavButtons()) {
                    handleNavButtons()
                }
                leftNavHolder.setOnClickListener {
                    var tab = mPager.currentItem
                    if (tab > 0) {
                        tab--
                        speak(getString(R.string.previous))
                        mPager.currentItem = tab
                    } else if (tab == 0) {
                        mPager.currentItem = tab
                    }
                }
                rightNavHolder.setOnClickListener {
                    var tab = mPager.currentItem
                    if (isPagerAdapterInitialized() &&
                        mPagerAdapter.count > 1 && tab != mPagerAdapter.count - 1
                    ) {
                        speak(getString(R.string.next))
                    }
                    tab++
                    mPager.currentItem = tab
                }
            }
        })

        return view
    }

    override fun nextPage() {
        if (shouldEnableNavButtons()) {
            var currentItem = mPager.currentItem
            if (currentItem == mPagerAdapter.count - 1) {
                mPager.currentItem = 0
            } else {
                mPager.currentItem = ++currentItem
            }
        }
    }

    private fun handleNavButtons() {
        enableNavButtons()
        if (shouldDisableLeftNavButton()) {
            leftNav.visibility = View.INVISIBLE
        }
        if (shouldDisableRightButton()) {
            rightNav.visibility = View.INVISIBLE
        }
    }

    private fun enableNavButtons() {
        leftNav.visibility = View.VISIBLE
        rightNav.visibility = View.VISIBLE
    }

    private fun isPagerAdapterInitialized(): Boolean = this::mPagerAdapter.isInitialized

    private fun shouldEnableNavButtons(): Boolean = isPagerAdapterInitialized() &&
        mPagerAdapter.count > 1

    private fun shouldDisableLeftNavButton(): Boolean = isPagerAdapterInitialized() &&
        mPager.currentItem == 0

    private fun shouldDisableRightButton(): Boolean = isPagerAdapterInitialized() &&
        mPager.currentItem >= mPagerAdapter.count - 1

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is PageFragmentListener) {
            mPageFragmentListener = context
        } else {
            throw RuntimeException("$context must implement PageFragmentListener")
        }
    }

    override fun onStop() {
        super.onStop()
        displayViewModel.currentPage.removeObservers(this@PageFragment)
    }

    fun speak(msg: String) {
        mPageFragmentListener.speak(msg)
    }

    interface PageFragmentListener {
        fun speak(msg: String)
    }

    companion object {

        fun newInstance(): PageFragment {
            return PageFragment()
        }
    }
}
