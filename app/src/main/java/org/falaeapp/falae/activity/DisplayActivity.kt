package org.falaeapp.falae.activity

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.widget.Toast
import org.falaeapp.falae.R
import org.falaeapp.falae.fragment.PageFragment
import org.falaeapp.falae.fragment.ViewPagerItemFragment
import org.falaeapp.falae.model.SpreadSheet
import org.falaeapp.falae.service.TextToSpeechService

class DisplayActivity : AppCompatActivity(), PageFragment.PageFragmentListener, ViewPagerItemFragment.ViewPagerItemFragmentListener {

    private lateinit var currentSpreadSheet: SpreadSheet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display)

        currentSpreadSheet = if (savedInstanceState == null) {
            intent.getParcelableExtra(SPREADSHEET)
        } else {
            savedInstanceState.classLoader = classLoader
            savedInstanceState.getParcelable(SPREADSHEET)
        }
        openPage(currentSpreadSheet.initialPage)
    }

    override fun openPage(linkTo: String) {
        val page = getPage(linkTo)
        if (page != null) {
            val fragment = PageFragment.newInstance(page)
            val fragmentManager = supportFragmentManager
            val fragmentTransaction = fragmentManager
                    .beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                            android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.page_container, fragment)
            if (currentSpreadSheet.initialPage != linkTo) {
                fragmentTransaction.addToBackStack(null)
            } else if (fragmentManager.backStackEntryCount > 0) {
                fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            }
            fragmentTransaction.commit()
            fragmentManager.executePendingTransactions()
        } else {
            Toast.makeText(this, getString(R.string.page_not_found), Toast.LENGTH_SHORT).show()
        }
    }

    private fun getPage(name: String) = currentSpreadSheet.pages.find { it.name == name }

    override fun speak(msg: String) {
        val intent = Intent(this, TextToSpeechService::class.java)
        intent.putExtra(TextToSpeechService.TEXT_TO_SPEECH_MESSAGE, msg)
        startService(intent)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_A) {
            val pageFragment = supportFragmentManager.fragments.lastOrNull { it is PageFragment }
            (pageFragment as PageFragment?)?.selectScannedItem()

        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putParcelable(SPREADSHEET, currentSpreadSheet)
    }

    companion object {
        const val SPREADSHEET = "SpreadSheet"
    }
}
