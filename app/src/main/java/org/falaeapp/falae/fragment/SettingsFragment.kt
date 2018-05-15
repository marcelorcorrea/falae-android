package org.falaeapp.falae.fragment

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.falaeapp.falae.R
import org.falaeapp.falae.util.Util
import org.falaeapp.falae.viewmodel.SettingsViewModel

class SettingsFragment : Fragment() {

    private lateinit var seekBar: SeekBar
    private lateinit var seekBarValue: TextView
    private lateinit var settingsViewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsViewModel = ViewModelProviders.of(activity!!).get(SettingsViewModel::class.java)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        val scanMode = view.findViewById<Switch>(R.id.scan_mode)
        val feedbackSound = view.findViewById<Switch>(R.id.feedback_sound)
        val automaticNextPage = view.findViewById<Switch>(R.id.automatic_next_page)

        seekBarValue = view.findViewById(R.id.seekbar_value) as TextView
        seekBar = view.findViewById(R.id.seekBar) as SeekBar


        settingsViewModel.loadScan()
        settingsViewModel.loadSeekBarProgress()
        settingsViewModel.loadFeedbackSound()
        settingsViewModel.loadAutomaticNextPage()

        settingsViewModel.isScanModeEnabled.observe(this, Observer {
            it?.let { scanMode.isChecked = it }
        })
        scanMode.setOnCheckedChangeListener { _, isChecked -> settingsViewModel.setScanModeChecked(isChecked) }

        settingsViewModel.isFeedbackSoundEnabled.observe(this, Observer {
            it?.let { feedbackSound.isChecked = it }
        })
        feedbackSound.setOnCheckedChangeListener { _, isChecked -> settingsViewModel.setFeedbackSoundChecked(isChecked) }

        settingsViewModel.isAutomaticNextPageEnabled.observe(this, Observer {
            it?.let { automaticNextPage.isChecked = it }
        })
        automaticNextPage.setOnCheckedChangeListener { _, isChecked -> settingsViewModel.setAutomaticNextPageChecked(isChecked) }

        settingsViewModel.setSeekBarProgress(0)

        settingsViewModel.seekBarProgress.observe(this, Observer { sk ->
            sk?.let {
                val seekBarProgress = if (it == 0) 1 else it
                seekBar.post {
                    setSeekBarText(seekBarProgress)
                    seekBar.progress = it - 1
                }
            }
        })

        seekBar.setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                        val actualProgress = progress + 1
                        setSeekBarText(actualProgress)
                        settingsViewModel.setSeekBarProgress(actualProgress)
                        val timeMillis: Long = (actualProgress * 500).toLong()
                        settingsViewModel.setScanModeDuration(timeMillis)
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar) {

                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar) {

                    }
                })
        setHasOptionsMenu(true)
        context?.let { context ->
            val btClearUserCache = view.findViewById<Button>(R.id.bt_clear_user_cache)
            btClearUserCache.setOnClickListener {
                Util.createDialog(context = context,
                        message = getString(R.string.confirm_clear_cache),
                        positiveText = getString(R.string.yes_option),
                        positiveClick = {
                            val message = if (true)
                                getString(R.string.public_images_removed)
                            else
                                getString(R.string.public_images_removed_error)

                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        },
                        negativeText = getString(R.string.no_option)
                )
            }
            val btClearPublicCache = view.findViewById<Button>(R.id.bt_clear_public_cache)
            btClearPublicCache.setOnClickListener {
                Util.createDialog(context = context,
                        message = getString(R.string.confirm_clear_cache),
                        positiveText = getString(R.string.yes_option),
                        positiveClick = {
                            val message = if (true)
                                getString(R.string.public_images_removed)
                            else
                                getString(R.string.public_images_removed_error)

                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        },
                        negativeText = getString(R.string.no_option)
                )
            }
        }
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
        fun newInstance(): SettingsFragment = SettingsFragment()
    }
}
