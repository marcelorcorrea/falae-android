package org.falaeapp.falae.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import org.falaeapp.falae.R
import org.falaeapp.falae.storage.SharedPreferencesUtils

class SettingsFragment : Fragment() {

    private lateinit var seekBar: SeekBar
    private lateinit var seekBarValue: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        val scanMode = view.findViewById(R.id.scan_mode) as Switch
        context?.let { context ->
            scanMode.isChecked = SharedPreferencesUtils.getBoolean(SCAN_MODE, context)

            scanMode.setOnCheckedChangeListener { _, isChecked -> SharedPreferencesUtils.storeBoolean(SCAN_MODE, isChecked, context) }

            seekBarValue = view.findViewById(R.id.seekbar_value) as TextView
            seekBar = view.findViewById(R.id.seekBar) as SeekBar

            SharedPreferencesUtils.storeInt(SEEK_BAR_PROGRESS, 0, context)
            val seekBarProgress = (SharedPreferencesUtils.getInt(SEEK_BAR_PROGRESS, context, 1))
                    // for legacy users
                    .let { if (it == 0) 1 else it }

            seekBar.post {
                setSeekBarText(seekBarProgress)
                seekBar.progress = seekBarProgress - 1
            }

            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    val actualProgress = progress + 1
                    setSeekBarText(actualProgress)
                    SharedPreferencesUtils.storeInt(SEEK_BAR_PROGRESS, actualProgress, context)
                    val timeMillis: Long = (actualProgress * 500).toLong()
                    SharedPreferencesUtils.storeLong(SCAN_MODE_DURATION, timeMillis, context)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {

                }
            })
        }
        setHasOptionsMenu(true)
        return view
    }

    private fun calculateSeekBarPosition(progress: Int): Float {
        val seekBarPosition = progress * (seekBar.width - 3 * seekBar.thumbOffset) / seekBar.max
        return seekBar.x + seekBarPosition.toFloat() + (seekBar.thumbOffset / 2).toFloat()
    }

    fun setSeekBarText(progress: Int) {
        seekBarValue.x = calculateSeekBarPosition(progress)
        seekBarValue.text = "${progress * 0.5f}"
    }

    companion object {

        const val SCAN_MODE = "scanMode"
        const val SEEK_BAR_PROGRESS = "seekBarProgress"
        const val SCAN_MODE_DURATION = "scanModeDuration"

        fun newInstance(): SettingsFragment = SettingsFragment()
    }
}
