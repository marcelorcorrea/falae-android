package org.falaeapp.falae.activity

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import org.falaeapp.falae.R
import org.falaeapp.falae.fragment.PageFragment
import org.falaeapp.falae.fragment.ViewPagerItemFragment
import org.falaeapp.falae.model.Page
import org.falaeapp.falae.model.SpreadSheet
import org.falaeapp.falae.service.TextToSpeechService
import org.falaeapp.falae.viewmodel.DisplayViewModel

class DisplayActivity : AppCompatActivity(), PageFragment.PageFragmentListener, ViewPagerItemFragment.ViewPagerItemFragmentListener {
    private lateinit var displayViewModel: DisplayViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display)

        val spreadSheet: SpreadSheet = intent.getParcelableExtra(SPREADSHEET)
        displayViewModel = ViewModelProviders.of(this).get(DisplayViewModel::class.java)
        displayViewModel.init(spreadSheet)

        displayViewModel.getInitialPage().observe(this, Observer {
            it?.let { initialPage ->
                changeFragment(initialPage)
            } ?: run {
                Toast.makeText(this, getString(R.string.page_not_found), Toast.LENGTH_SHORT).show()
            }
        })
        displayViewModel.currentPage.observe(this, Observer {
            it?.let { page ->
                changeFragment(page, true)
            } ?: run {
                Toast.makeText(this, getString(R.string.page_not_found), Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun changeFragment(page: Page, addToBackStack: Boolean = false) {
        val fragment = PageFragment.newInstance(page)
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                        android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.page_container, fragment)
        if (addToBackStack) {
            fragmentTransaction.addToBackStack(null)
        }
        if (fragmentManager.backStackEntryCount > 0) {
            fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
        fragmentTransaction.commit()
        fragmentManager.executePendingTransactions()
    }

    override fun speak(msg: String) {
        val intent = Intent(this, TextToSpeechService::class.java)
        intent.putExtra(TextToSpeechService.TEXT_TO_SPEECH_MESSAGE, msg)
        startService(intent)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
    }

    companion object {
        const val SPREADSHEET = "SpreadSheet"
    }
}
