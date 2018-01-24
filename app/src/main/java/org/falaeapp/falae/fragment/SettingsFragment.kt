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

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.fragment_settings, container, false)
        val scanMode = view.findViewById(R.id.scan_mode) as Switch
        scanMode.isChecked = SharedPreferencesUtils.getBoolean(SCAN_MODE, context)

        scanMode.setOnCheckedChangeListener { _, isChecked -> SharedPreferencesUtils.storeBoolean(SCAN_MODE, isChecked, context) }

        seekBarValue = view.findViewById(R.id.seekbar_value) as TextView
        seekBar = view.findViewById(R.id.seekBar) as SeekBar

        val seekBarProgress = SharedPreferencesUtils.getInt(SEEK_BAR_PROGRESS, context)
        seekBar.post {
            setSeekBarText(seekBarProgress)
            seekBar.progress = seekBarProgress
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                setSeekBarText(progress)
                SharedPreferencesUtils.storeInt(SEEK_BAR_PROGRESS, progress, context)
                val timeMillis = progress * 500
                SharedPreferencesUtils.storeInt(SCAN_MODE_DURATION, timeMillis, context)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })

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