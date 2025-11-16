package com.edu.wszib.findyourpet.inputmasks

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

class DateInputMask(val input: EditText) {

    fun listen() {
        // Attach text watcher to listen for date input changes
        input.addTextChangedListener(mDateEntryWatcher)
    }

    private val mDateEntryWatcher = object : TextWatcher {

        var edited = false
        val dividerCharacter = "/"

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            // Prevent recursive triggering when text is programmatically changed
            if (edited) {
                edited = false
                return
            }

            var working = getEditText()

            // Insert or remove divider after the day and month positions
            working = manageDateDivider(working, 2, start, before)
            working = manageDateDivider(working, 5, start, before)

            // Ensure that the year part does not exceed 4 digits
            val indexOfSecondSlash = working.indexOf("/", 3)
            if (indexOfSecondSlash != -1 && working.length - indexOfSecondSlash > 5) {
                working = working.substring(0, indexOfSecondSlash + 5)
            }

            edited = true
            input.setText(working)
            input.setSelection(input.text.length)
        }

        private fun manageDateDivider(
            working: String,
            position: Int,
            start: Int,
            before: Int
        ): String {
            // Automatically add or remove divider depending on edit position
            if (working.length == position) {
                return if (before <= position && start < position)
                    working + dividerCharacter
                else
                    working.dropLast(1)
            }
            return working
        }

        private fun getEditText(): String {
            // Limit input to maximum length of DD/MM/YYYY
            return if (input.text.length >= 10)
                input.text.toString().substring(0, 10)
            else
                input.text.toString()
        }

        override fun afterTextChanged(s: Editable) {}
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    }
}
