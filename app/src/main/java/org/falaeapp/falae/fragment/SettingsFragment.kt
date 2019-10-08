package org.falaeapp.falae.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import org.falaeapp.falae.R
import org.falaeapp.falae.util.Util
import org.falaeapp.falae.viewmodel.SettingsViewModel
import org.falaeapp.falae.viewmodel.UserViewModel

class SettingsFragment : Fragment() {

    private lateinit var seekBar: SeekBar
    private lateinit var seekBarValue: TextView
    private lateinit var scanMode: Switch
    private lateinit var feedbackSound: Switch
    private lateinit var automaticNextPage: Switch
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var userViewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsViewModel = ViewModelProvider(activity!!).get(SettingsViewModel::class.java)
        userViewModel = ViewModelProvider(activity!!).get(UserViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        scanMode = view.findViewById(R.id.scan_mode)
        feedbackSound = view.findViewById(R.id.feedback_sound)
        automaticNextPage = view.findViewById(R.id.automatic_next_page)
        seekBarValue = view.findViewById(R.id.seekbar_value) as TextView
        seekBar = view.findViewById(R.id.seekBar) as SeekBar

        settingsViewModel.loadScan()
        settingsViewModel.loadSeekBarProgress()
        settingsViewModel.loadFeedbackSound()
        settingsViewModel.loadAutomaticNextPage()

        scanMode.setOnCheckedChangeListener { _, isChecked -> settingsViewModel.setScanModeChecked(isChecked) }
        feedbackSound.setOnCheckedChangeListener { _, isChecked -> settingsViewModel.setFeedbackSoundChecked(isChecked) }
        automaticNextPage.setOnCheckedChangeListener { _, isChecked ->
            settingsViewModel.setAutomaticNextPageChecked(isChecked)
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
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
        val btClearUserCache = view.findViewById<Button>(R.id.bt_clear_user_cache)
        btClearUserCache.setOnClickListener {
            onClickCache(getString(R.string.confirm_clear_user_cache)) {
                userViewModel.clearUserCache()
            }
        }
        val btClearPublicCache = view.findViewById<Button>(R.id.bt_clear_public_cache)
        btClearPublicCache.setOnClickListener {
            onClickCache(getString(R.string.confirm_clear_public_cache)) {
                userViewModel.clearPublicCache()
            }
        }
        setHasOptionsMenu(true)

        observeScanMode()
        observeFeedbackSound()
        observeAutomaticNextPage()
        observeClearCache()
        observeSeekBarProgress()
        observeCurrentUser(view)
        return view
    }

    private fun observeCurrentUser(view: View) {
        userViewModel.currentUser.observe(viewLifecycleOwner, Observer { user ->
            if (user != null && user.isSampleUser()) {
                val cacheLayout = view.findViewById<RelativeLayout>(R.id.cache_layout)
                cacheLayout.visibility = View.INVISIBLE
            }
        })
    }

    private fun observeSeekBarProgress() {
        settingsViewModel.seekBarProgress.observe(viewLifecycleOwner, Observer { sk ->
            sk?.let {
                val seekBarProgress = if (it == 0) 1 else it
                seekBar.post {
                    setSeekBarText(seekBarProgress)
                    seekBar.progress = it - 1
                }
            }
        })
    }

    private fun observeClearCache() {
        userViewModel.clearCache.observe(viewLifecycleOwner, Observer { event ->
            event?.getContentIfNotHandled()?.let { result ->
                val message =
                    if (result) getString(R.string.images_removed) else getString(R.string.images_removed_error)
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun observeAutomaticNextPage() {
        settingsViewModel.isAutomaticNextPageEnabled.observe(viewLifecycleOwner, Observer { isEnabled ->
            automaticNextPage.isChecked = isEnabled
        })
    }

    private fun observeFeedbackSound() {
        settingsViewModel.isFeedbackSoundEnabled.observe(viewLifecycleOwner, Observer { isEnabled ->
            feedbackSound.isChecked = isEnabled
        })
    }

    private fun observeScanMode() {
        settingsViewModel.isScanModeEnabled.observe(viewLifecycleOwner, Observer { isEnabled ->
            scanMode.isChecked = isEnabled
            automaticNextPage.isEnabled = isEnabled
            feedbackSound.isEnabled = isEnabled
            seekBar.isEnabled = isEnabled
        })
    }

    private fun onClickCache(confirmMsg: String, onClick: () -> Unit) {
        val dialog = Util.createDialog(
            context = context!!,
            message = confirmMsg,
            positiveText = getString(R.string.yes_option),
            positiveClick = onClick,
            negativeText = getString(R.string.no_option)
        )
        dialog.show()
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
