package com.marcelorcorrea.falae.activity

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.widget.Toast
import com.marcelorcorrea.falae.R
import com.marcelorcorrea.falae.fragment.PageFragment
import com.marcelorcorrea.falae.fragment.ViewPagerItemFragment
import com.marcelorcorrea.falae.model.Page
import com.marcelorcorrea.falae.model.SpreadSheet
import com.marcelorcorrea.falae.service.TextToSpeechService

class DisplayActivity : AppCompatActivity(), PageFragment.PageFragmentListener, ViewPagerItemFragment.ViewPagerItemFragmentListener {

    private var currentSpreadSheet: SpreadSheet? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display)

        currentSpreadSheet = intent.getParcelableExtra(SPREADSHEET)
        currentSpreadSheet?.let { openPage(it.initialPage) }
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
            if (currentSpreadSheet!!.initialPage != linkTo) {
                fragmentTransaction.addToBackStack(null)
            } else if (fragmentManager.backStackEntryCount > 0) {
                fragmentManager.popBackStackImmediate()
            }
            fragmentTransaction.commit()
            fragmentManager.executePendingTransactions()
        } else {
            Toast.makeText(this, getString(R.string.page_not_found), Toast.LENGTH_SHORT).show()
        }
    }

    private fun getPage(name: String): Page? = currentSpreadSheet!!.pages.firstOrNull { it.name == name }

    override fun speak(msg: String) {
        val intent = Intent(this, TextToSpeechService::class.java)
        intent.putExtra(TextToSpeechService.TEXT_TO_SPEECH_MESSAGE, msg)
        startService(intent)
    }

    override fun onBackPressed() {
        // The backPressed should has the same behavior as "home" clicking (when you're not in home!)
//        if (supportFragmentManager.backStackEntryCount > 0) {
//            currentSpreadSheet?.let { openPage(it.initialPage) }
//        } else {
            super.onBackPressed()
            overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
//        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_A) {
            val pageFragment = supportFragmentManager.fragments.lastOrNull { it is PageFragment }
            (pageFragment as PageFragment?)?.selectScannedItem()

        }
        return super.onKeyDown(keyCode, event)
    }

    companion object {

        val SPREADSHEET = "SpreadSheet"
    }
}
