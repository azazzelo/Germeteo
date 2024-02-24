package com.example.germeteo

import android.app.AlertDialog
import android.content.Context
import android.widget.Toast

object DialogManager {
    fun locationSettingsDialog(context: Context, listener: Listener) {
        val builder = AlertDialog.Builder(context)
        val dialog = builder.create()
        dialog.setTitle("Enable location?")
        dialog.setMessage("Location disabled. Do you want enable location?" +
                " Location is required to get weather data for your location.")
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK") {_,_ ->
            listener.onClick()
            dialog.dismiss()

        }
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel") {_,_ ->
            dialog.dismiss()

        }
        dialog.show()
    }

    interface Listener {
        fun onClick()
    }
}