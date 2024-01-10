package com.example.familyshoppingapp

import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast

object CustomToast {
    fun showCustomToast(context: Context, message: String, duration: Int) {
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.custom_toast, null)

        layout.findViewById<TextView>(R.id.custom_toast_text).text = message

        with (Toast(context)) {
            this.duration = duration
            view = layout
            show()
        }
    }
}