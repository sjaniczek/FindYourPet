package com.edu.wszib.findyourpet.inputmasks

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

class TimeInputMask(val input: EditText) {

    fun listen() {
        input.addTextChangedListener(mTimeEntryWatcher)
    }

    private val mTimeEntryWatcher = object : TextWatcher {

        var edited = false
        val dividerCharacter = ":"

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            if (edited) {
                edited = false
                return
            }

            var working = getEditText()

            working = manageTimeDivider(working, 2, start, before)

            edited = true
            input.setText(working)
            input.setSelection(input.text.length)
        }

        private fun manageTimeDivider(working: String, position: Int, start: Int, before: Int): String {
            if (working.length == position) {
                return if (before <= position && start < position)
                    working + dividerCharacter
                else
                    working.dropLast(1)
            }
            return working
        }

        private fun getEditText(): String {
            return if (input.text.length >= 5)
                input.text.toString().substring(0, 5)
            else
                input.text.toString()
        }

        override fun afterTextChanged(s: Editable) {}
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    }
}