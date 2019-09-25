package org.falaeapp.falae.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import android.util.SparseArray
import android.view.ViewGroup
import org.falaeapp.falae.fragment.ViewPagerItemFragment
import org.falaeapp.falae.model.Page
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.min
import kotlin.math.roundToInt


class ItemPagerAdapter(fm: FragmentManager, private val page: Page, private val marginWidth: Int) : FragmentStatePagerAdapter(fm) {
    private val pageCount: Int
    private val fragmentReferences = SparseArray<WeakReference<Fragment>>()

    init {
        pageCount = calculatePageCount()
    }

    override fun getItem(position: Int): Fragment {
        return fragmentReferences.get(position)?.get() ?: run {
            val items = page.items
            val itemsPerPage = page.columns * page.rows
            val fromIndex = position * itemsPerPage
            val subList = items.subList(fromIndex, min(fromIndex + itemsPerPage, items.size))
            val newInstance: Fragment = ViewPagerItemFragment.newInstance(ArrayList(subList), page.columns, page.rows, marginWidth)
            fragmentReferences.put(position, WeakReference(newInstance))
            newInstance
        }
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        fragmentReferences.remove(position)
        super.destroyItem(container, position, `object`)
    }

    override fun getCount(): Int = pageCount

    private fun calculatePageCount(): Int {
        val numberOfPages = page.items.size.toDouble() / (page.columns * page.rows)
        return if (numberOfPages == numberOfPages.roundToInt().toDouble()) {
            numberOfPages.toInt()
        } else (numberOfPages + 0.5).roundToInt()
    }
}