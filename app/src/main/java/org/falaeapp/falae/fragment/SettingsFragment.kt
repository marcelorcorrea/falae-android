package org.falaeapp.falae.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.falaeapp.falae.R
import org.falaeapp.falae.databinding.FragmentSettingsBinding
import org.falaeapp.falae.util.Util
import org.falaeapp.falae.viewmodel.SettingsViewModel
import org.falaeapp.falae.viewmodel.UserViewModel

class SettingsFragment : Fragment() {

    private lateinit var seekBar: SeekBar
    private lateinit var seekBarValue: TextView
    private lateinit var scanMode: SwitchCompat
    private lateinit var feedbackSound: SwitchCompat
    private lateinit var automaticNextPage: SwitchCompat
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var userViewModel: UserViewModel
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val requireActivity = requireActivity()
        val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity.application)
        settingsViewModel = ViewModelProvider(requireActivity, factory)[SettingsViewModel::class.java]
        userViewModel = ViewModelProvider(requireActivity, factory)[UserViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val view = binding.root
        scanMode = binding.scanMode
        feedbackSound = binding.feedbackSound
        automaticNextPage = binding.automaticNextPage
        seekBarValue = binding.seekbarValue
        seekBar = binding.seekBar

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
        binding.btClearUserCache.setOnClickListener {
            onClickCache(getString(R.string.confirm_clear_user_cache)) {
                userViewModel.clearUserCache()
            }
        }
        binding.btClearPublicCache.setOnClickListener {
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
        observeCurrentUser()
        return view
    }

    private fun observeCurrentUser() {
        userViewModel.currentUser.observe(
            viewLifecycleOwner
        ) { user ->
            if (user != null && user.isSampleUser()) {
                binding.cacheLayout.visibility = View.INVISIBLE
            }
        }
    }

    private fun observeSeekBarProgress() {
        settingsViewModel.seekBarProgress.observe(
            viewLifecycleOwner
        ) { sk ->
            sk?.let {
                val seekBarProgress = if (it == 0) 1 else it
                seekBar.post {
                    setSeekBarText(seekBarProgress)
                    seekBar.progress = it - 1
                }
            }
        }
    }

    private fun observeClearCache() {
        userViewModel.clearCache.observe(
            viewLifecycleOwner
        ) { event ->
            event?.getContentIfNotHandled()?.let { result ->
                val message =
                    if (result) getString(R.string.images_removed) else getString(R.string.images_removed_error)
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeAutomaticNextPage() {
        settingsViewModel.isAutomaticNextPageEnabled.observe(
            viewLifecycleOwner
        ) { isEnabled ->
            automaticNextPage.isChecked = isEnabled
        }
    }

    private fun observeFeedbackSound() {
        settingsViewModel.isFeedbackSoundEnabled.observe(
            viewLifecycleOwner
        ) { isEnabled ->
            feedbackSound.isChecked = isEnabled
        }
    }

    private fun observeScanMode() {
        settingsViewModel.isScanModeEnabled.observe(
            viewLifecycleOwner
        ) { isEnabled ->
            scanMode.isChecked = isEnabled
            automaticNextPage.isEnabled = isEnabled
            feedbackSound.isEnabled = isEnabled
            seekBar.isEnabled = isEnabled
        }
    }

    private fun onClickCache(confirmMsg: String, onClick: () -> Unit) {
        val dialog = Util.createDialog(
            context = requireContext(),
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
