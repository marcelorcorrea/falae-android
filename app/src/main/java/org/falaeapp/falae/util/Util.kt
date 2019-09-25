package org.falaeapp.falae.util

import android.content.Context
import androidx.appcompat.app.AlertDialog
import org.falaeapp.falae.R

object Util {

    fun createDialog(context: Context,
                     title: String = "",
                     message: String,
                     neutralText: String = "",
                     neutral: () -> Unit = {},
                     positiveText: String = "",
                     positiveClick: () -> Unit = {},
                     negativeText: String = "",
                     negativeClick: () -> Unit = {}): AlertDialog {
        val builder = AlertDialog.Builder(context)
                .setMessage(message)
        if (title.isNotEmpty()) {
            builder.setTitle(title)
        }
        if (neutralText.isNotEmpty()) {
            builder.setNeutralButton(neutralText) { dialog, _ ->
                neutral()
                dialog.dismiss()

            }
        }
        if (negativeText.isNotEmpty()) {
            builder.setNegativeButton(negativeText) { dialog, _ ->
                negativeClick()
                dialog.dismiss()
            }
        }
        if (positiveText.isNotEmpty()) {
            builder.setPositiveButton(positiveText) { dialog, _ ->
                positiveClick()
                dialog.dismiss()
            }
        }
        val alertDialog = builder.create()
        alertDialog.setOnShowListener {
            val buttonPositive = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val buttonNegative = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            buttonPositive.setTextColor(context.resources.getColor(R.color.colorAccent))
            buttonNegative.setTextColor(context.resources.getColor(R.color.colorAccent))
        }
        return alertDialog
    }

}