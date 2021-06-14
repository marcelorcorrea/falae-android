package org.falaeapp.falae.activity

import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import org.falaeapp.falae.R
import org.falaeapp.falae.fragment.PageFragment
import org.falaeapp.falae.fragment.ViewPagerItemFragment
import org.falaeapp.falae.model.Page
import org.falaeapp.falae.model.SpreadSheet
import org.falaeapp.falae.service.TextToSpeechService
import org.falaeapp.falae.viewmodel.DisplayViewModel

class DisplayActivity : AppCompatActivity(), PageFragment.PageFragmentListener,
    ViewPagerItemFragment.ViewPagerItemFragmentListener {
    private lateinit var displayViewModel: DisplayViewModel
    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display)

        val spreadSheet: SpreadSheet? = intent.getParcelableExtra(SPREADSHEET)
        val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        displayViewModel = ViewModelProvider(this, factory).get(DisplayViewModel::class.java)
        spreadSheet?.let {
            displayViewModel.init(it)
        }

        displayViewModel.pageToOpen.observe(this, Observer {
            it?.let { page ->
                changeFragment(page, page.initialPage.not())
            } ?: run {
                Toast.makeText(this, getString(R.string.page_not_found), Toast.LENGTH_SHORT).show()
            }
        })
        mediaPlayer = MediaPlayer.create(this, R.raw.click_sound)
    }

    private fun changeFragment(page: Page, addToBackStack: Boolean = false) {
        val fragment = PageFragment.newInstance()
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager
            .beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out
            )
            .replace(R.id.page_container, fragment)
        if (addToBackStack) {
            fragmentTransaction.addToBackStack(page.name)
        } else if (fragmentManager.backStackEntryCount > 0) {
            fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
        fragmentTransaction.commit()
        fragmentManager.executePendingTransactions()
        displayViewModel.setCurrentPage(page)
    }

    override fun speak(msg: String) {
        val intent = Intent(this, TextToSpeechService::class.java)
        intent.putExtra(TextToSpeechService.TEXT_TO_SPEECH_MESSAGE, msg)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    override fun playFeedbackSound() {
        mediaPlayer.start()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
    }

    companion object {
        const val SPREADSHEET = "SpreadSheet"
    }
}
