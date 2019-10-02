package org.falaeapp.falae.fragment

import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.gridlayout.widget.GridLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.squareup.picasso.Picasso
import org.falaeapp.falae.R
import org.falaeapp.falae.model.Category
import org.falaeapp.falae.model.Item
import org.falaeapp.falae.viewmodel.DisplayViewModel
import org.falaeapp.falae.viewmodel.SettingsViewModel
import java.util.ArrayList
import java.util.Timer
import java.util.TimerTask
import kotlin.math.roundToInt

class ViewPagerItemFragment : Fragment() {
    private var mItems: List<Item> = emptyList()
    private var mItemsLayout: List<FrameLayout> = emptyList()
    private lateinit var mListener: ViewPagerItemFragmentListener
    private var mColumns: Int = 0
    private var mRows: Int = 0
    private var mMarginWidth: Int = 0
    private var currentItemSelectedFromScan = -1
    private lateinit var mGridLayout: GridLayout
    private lateinit var displayViewModel: DisplayViewModel
    private lateinit var settingsViewModel: SettingsViewModel
    private var isScanModeEnabled: Boolean = false
    private var shouldPlayFeedbackSound: Boolean = false
    private var shouldCallNextPage: Boolean = false
    private var delay: Long = 0
    private var mTimer: Timer? = null
        get() {
            if (field == null) {
                field = Timer()
            }
            return field
        }
    private var timerTask: TimerTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        displayViewModel = ViewModelProvider(activity!!).get(DisplayViewModel::class.java)
        settingsViewModel = ViewModelProvider(activity!!).get(SettingsViewModel::class.java)
        arguments?.let { arguments ->
            mItems = arguments.getParcelableArrayList(ITEMS_PARAM) ?: emptyList()
            mColumns = arguments.getInt(COLUMNS_PARAM)
            mRows = arguments.getInt(ROWS_PARAM)
            mMarginWidth = arguments.getInt(MARGIN_WIDTH)
        } ?: return
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_view_pager_item, container, false)
        mGridLayout = view.findViewById(R.id.grid_layout) as GridLayout
        mGridLayout.alignmentMode = GridLayout.ALIGN_BOUNDS
        mGridLayout.columnCount = mColumns
        mGridLayout.rowCount = mRows

        val layoutDimensions = calculateLayoutDimensions()

        mItemsLayout = mItems.map { item ->
            val layout = generateLayout(inflater, item, layoutDimensions)
            mGridLayout.addView(layout)
            layout
        }
        observeFeedbackSound()
        observeAutomaticNextPage()
        return view
    }

    private fun observeAutomaticNextPage() {
        settingsViewModel.shouldEnableAutomaticNextPage().observe(viewLifecycleOwner, Observer { result ->
            shouldCallNextPage = result
        })
    }

    private fun observeFeedbackSound() {
        settingsViewModel.shouldEnableFeedbackSound().observe(viewLifecycleOwner, Observer { result ->
            shouldPlayFeedbackSound = result
        })
    }

    private fun observeScanMode() {
        settingsViewModel.shouldEnableScanMode().observe(viewLifecycleOwner, Observer { pair ->
            isScanModeEnabled = pair.first
            if (isScanModeEnabled) {
                delay = pair.second
                doPageScan()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        observeScanMode()
    }

    override fun onPause() {
        super.onPause()
        stopPageScan()
    }

    private fun generateLayout(inflater: LayoutInflater, item: Item, layoutDimensions: Point): FrameLayout {
        val frameLayout = inflater.inflate(R.layout.item, null, false) as FrameLayout
        val name = frameLayout.findViewById(R.id.item_name) as TextView
        val imageView = frameLayout.findViewById(R.id.item_image_view) as ImageView
        val linkPage = frameLayout.findViewById(R.id.link_page) as ImageView

        frameLayout.layoutParams = FrameLayout.LayoutParams(layoutDimensions.x, layoutDimensions.y)
        val drawable = createBackgroundDrawable(item)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            frameLayout.setBackgroundDrawable(drawable)
        } else {
            frameLayout.background = drawable
        }
        frameLayout.setOnClickListener {
            var itemSelected = item
            if (isScanModeEnabled) {
                itemSelected = mItems[currentItemSelectedFromScan]
            }
            onItemClicked(itemSelected)
        }
        name.text = item.name
        name.post {
            val imageSize = calculateImageSize(layoutDimensions.x, layoutDimensions.y, name, imageView)
            if (item.category == Category.SUBJECT || item.category == Category.OTHER) {
                name.setTextColor(Color.BLACK)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    linkPage.setImageDrawable(context?.resources?.getDrawable(R.drawable.ic_launch_black_48dp))
                } else {
                    linkPage.setImageDrawable(context?.getDrawable(R.drawable.ic_launch_black_48dp))
                }
            }
            if (item.imgSrc.isNotEmpty()) {
                if (imageSize > 0 && context != null) {
                    Picasso.get()
                        .load(item.imgSrc)
                        .placeholder(R.drawable.ic_image_black_48dp)
                        .error(R.drawable.ic_broken_image_black_48dp)
                        .resize(imageSize, imageSize)
                        .centerCrop()
                        .into(imageView)
                }
            } else {
                Toast.makeText(context, getString(R.string.picasso_load_error), Toast.LENGTH_SHORT).show()
            }
            if (item.linkTo != null && item.linkTo.isNotEmpty()) {
                linkPage.visibility = View.VISIBLE
            }
        }
        return frameLayout
    }

    private fun onItemClicked(item: Item) {
        mListener.speak(item.speech)
        if (item.linkTo != null && item.linkTo.isNotEmpty()) {
            displayViewModel.openPage(item.linkTo)
        }
    }

    private fun calculateLayoutDimensions(): Point {
        val metrics = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(metrics)
        val widthDimension = ((metrics.widthPixels - mMarginWidth) / mColumns).toFloat().roundToInt()
        val heightDimension = (metrics.heightPixels / mRows).toFloat().roundToInt()
        return Point(widthDimension, heightDimension)
    }

    private fun calculateImageSize(layoutWidth: Int, layoutHeight: Int, name: TextView, imageView: ImageView): Int {
        val nameTopMargin = (name.layoutParams as ConstraintLayout.LayoutParams).topMargin
        val nameBoxHeight = nameTopMargin + name.lineHeight * name.lineCount
        val availableHeight = layoutHeight - nameBoxHeight
        return if (availableHeight > layoutWidth) {
            val imageLeftMargin = (imageView.layoutParams as ConstraintLayout.LayoutParams).leftMargin
            val imageRightMargin = (imageView.layoutParams as ConstraintLayout.LayoutParams).rightMargin
            layoutWidth - (imageLeftMargin + imageRightMargin)
        } else {
            val imageTopMargin = (imageView.layoutParams as ConstraintLayout.LayoutParams).topMargin
            val imageBottomMargin = (imageView.layoutParams as ConstraintLayout.LayoutParams).bottomMargin
            availableHeight - (imageTopMargin + imageBottomMargin)
        }
    }

    private fun createBackgroundDrawable(item: Item): Drawable {
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.setStroke(2, Color.BLACK)
        drawable.cornerRadius = 8f
        drawable.setColor(item.category.color())
        return drawable
    }

    private fun doPageScan() {
        currentItemSelectedFromScan = -1
        if (isScanModeEnabled && userVisibleHint) {
            timerTask = object : TimerTask() {
                override fun run() {
                    try {
                        currentItemSelectedFromScan++
                        activity?.runOnUiThread {
                            if (currentItemSelectedFromScan > mItemsLayout.size - 1) {
                                currentItemSelectedFromScan = 0
                                if (shouldCallNextPage) {
                                    val fragment = parentFragment
                                    if (fragment is PageInteractionListener) {
                                        fragment.nextPage()
                                    }
                                }
                            }
                            playFeedbackSound()
                            highlightCurrentItem()
                            removeHighlightedItem(currentItemSelectedFromScan - 1)
                        }
                    } catch (e: Exception) {
                        Log.e(javaClass.name, "ViewPagerItemFragment: ${e.message}")
                    }
                }
            }
            mTimer?.schedule(timerTask, 0, delay)
        }
    }

    private fun stopPageScan() {
        removeHighlightedItem(currentItemSelectedFromScan)
        timerTask?.cancel()
    }

    private fun playFeedbackSound() {
        if (shouldPlayFeedbackSound) {
            mListener.playFeedbackSound()
        }
    }

    private fun highlightCurrentItem() {
        if (context != null && currentItemSelectedFromScan < mItemsLayout.size) {
            mItemsLayout[currentItemSelectedFromScan].foreground =
                context?.resources?.getDrawable(R.drawable.pressed_color)
        }
    }

    private fun removeHighlightedItem(currentItemSelectedFromScan: Int) {
        var previousItem = currentItemSelectedFromScan
        if (previousItem < 0) {
            previousItem = mItemsLayout.size - 1
        }
        if (context != null && previousItem < mItemsLayout.size) {
            mItemsLayout[previousItem].foreground = context?.resources?.getDrawable(R.drawable.normal_color)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is ViewPagerItemFragmentListener) {
            mListener = context
        } else {
            throw RuntimeException("$context must implement ViewPagerItemFragmentListener")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mTimer?.let {
            it.purge()
            it.cancel()
        }
        mTimer = null
    }

    interface ViewPagerItemFragmentListener {
        fun speak(msg: String)
        fun playFeedbackSound()
    }

    interface PageInteractionListener {
        fun nextPage()
    }

    companion object {
        private const val ITEMS_PARAM = "items"
        private const val COLUMNS_PARAM = "columns"
        private const val ROWS_PARAM = "rows"
        private const val MARGIN_WIDTH = "marginWidth"

        fun newInstance(items: ArrayList<Item>, columns: Int, rows: Int, width: Int): ViewPagerItemFragment {
            val fragment = ViewPagerItemFragment()
            val args = Bundle()
            args.putParcelableArrayList(ITEMS_PARAM, items)
            args.putInt(COLUMNS_PARAM, columns)
            args.putInt(ROWS_PARAM, rows)
            args.putInt(MARGIN_WIDTH, width)
            fragment.arguments = args
            return fragment
        }
    }
}
