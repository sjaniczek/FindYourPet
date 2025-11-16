package com.edu.wszib.findyourpet.inputmasks

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

class TimeInputMask(val input: EditText) {

    fun listen() {
        // Attach text watcher to format time as HH:MM
        input.addTextChangedListener(mTimeEntryWatcher)
    }

    private val mTimeEntryWatcher = object : TextWatcher {

        var edited = false
        val dividerCharacter = ":"

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            // Prevent infinite loop caused by programmatically setting text
            if (edited) {
                edited = false
                return
            }

            var working = getEditText()

            // Insert or remove ':' after hours part
            working = manageTimeDivider(working, 2, start, before)

            edited = true
            input.setText(working)
            input.setSelection(input.text.length)
        }

        private fun manageTimeDivider(
            working: String,
            position: Int,
            start: Int,
            before: Int
        ): String {
            // Automatically add/remove ':' at the correct position
            if (working.length == position) {
                return if (before <= position && start < position)
                    working + dividerCharacter
                else
                    working.dropLast(1)
            }
            return working
        }

        private fun getEditText(): String {
            // Limit input length to HH:MM
            return if (input.text.length >= 5)
                input.text.toString().substring(0, 5)
            else
                input.text.toString()
        }

        override fun afterTextChanged(s: Editable) {}
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    }
}
