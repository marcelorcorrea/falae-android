package org.falaeapp.falae.adapter

import android.util.SparseArray
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.falaeapp.falae.fragment.ViewPagerItemFragment
import org.falaeapp.falae.model.Page
import java.lang.ref.WeakReference
import java.util.ArrayList
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Adapter para ViewPager2 que substitui o depreciado FragmentStatePagerAdapter
 */
class ItemPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    private val page: Page,
    private val marginWidth: Int
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    private val pageCount: Int
    private val fragmentReferences = SparseArray<WeakReference<Fragment>>()

    init {
        pageCount = calculatePageCount()
    }

    override fun createFragment(position: Int): Fragment {
        return fragmentReferences.get(position)?.get() ?: run {
            val items = page.items
            val itemsPerPage = page.columns * page.rows
            val fromIndex = position * itemsPerPage
            val subList = items.subList(fromIndex, min(fromIndex + itemsPerPage, items.size))
            val newInstance: Fragment =
                ViewPagerItemFragment.newInstance(ArrayList(subList), page.columns, page.rows, marginWidth)
            fragmentReferences.put(position, WeakReference(newInstance))
            newInstance
        }
    }

    override fun getItemCount(): Int = pageCount

    private fun calculatePageCount(): Int {
        val numberOfPages = page.items.size.toDouble() / (page.columns * page.rows)
        return if (numberOfPages == numberOfPages.roundToInt().toDouble()) {
            numberOfPages.toInt()
        } else (numberOfPages + 0.5).roundToInt()
    }

    // Método para limpar referências quando o ViewPager2 é destruído
    fun clearReferences() {
        fragmentReferences.clear()
    }
}