package org.falaeapp.falae.fragment

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageView
import org.falaeapp.falae.R
import org.falaeapp.falae.adapter.ItemPagerAdapter
import org.falaeapp.falae.model.Page


class PageFragment : Fragment() {

    private var page: Page? = null
    private lateinit var mPageFragmentListener: PageFragmentListener
    private lateinit var mPager: ViewPager
    private lateinit var mPagerAdapter: ItemPagerAdapter
    private lateinit var leftNav: ImageView
    private lateinit var rightNav: ImageView
    private lateinit var leftNavHolder: FrameLayout
    private lateinit var rightNavHolder: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        page = if (savedInstanceState != null) {
            savedInstanceState.getParcelable(PAGE_PARAM)
        } else {
            arguments.getParcelable(PAGE_PARAM)
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putParcelable(PAGE_PARAM, page)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.fragment_page, container, false)
        leftNav = view.findViewById(R.id.left_nav) as ImageView
        rightNav = view.findViewById(R.id.right_nav) as ImageView
        leftNavHolder = view.findViewById(R.id.left_nav_holder) as FrameLayout
        rightNavHolder = view.findViewById(R.id.right_nav_holder) as FrameLayout

        val vto = view.viewTreeObserver
        vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    view.viewTreeObserver.removeGlobalOnLayoutListener(this)
                } else {
                    view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
                mPager = view.findViewById(R.id.pager) as ViewPager
                val navHoldersSize = java.lang.Double.valueOf(mPager.measuredWidth * 0.065)!!.toInt()
                leftNav.layoutParams.width = navHoldersSize
                leftNav.layoutParams.height = navHoldersSize
                rightNav.layoutParams.width = navHoldersSize
                rightNav.layoutParams.height = navHoldersSize
                leftNavHolder.layoutParams.width = navHoldersSize
                rightNavHolder.layoutParams.width = navHoldersSize
                page?.let {
                    mPagerAdapter = ItemPagerAdapter(childFragmentManager, it, navHoldersSize * 2)
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
                            mPagerAdapter.count > 1 && tab != mPagerAdapter.count - 1) {
                        speak(getString(R.string.next))
                    }
                    tab++
                    mPager.currentItem = tab
                }
            }
        })

        return view
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

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is PageFragmentListener) {
            mPageFragmentListener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement PageFragmentListener")
        }
    }

    fun speak(msg: String) {
        mPageFragmentListener.speak(msg)
    }

    interface PageFragmentListener {
        fun speak(msg: String)
    }

    companion object {
        private const val PAGE_PARAM = "pageParam"

        fun newInstance(page: Page): PageFragment {
            val fragment = PageFragment()
            val args = Bundle()
            args.putParcelable(PAGE_PARAM, page)
            fragment.arguments = args
            return fragment
        }
    }
}
