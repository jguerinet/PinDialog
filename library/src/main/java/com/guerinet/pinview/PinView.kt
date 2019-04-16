/*
 * Copyright 2017-2019 Julien Guerinet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.guerinet.pinview

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatEditText

/**
 * Allows the user to enter a decimal pin code
 * @author Julien Guerinet
 * @since 1.0.0
 */
class PinView : LinearLayout, View.OnFocusChangeListener {

    /** Called when a text is entered */
    var onEntered: ((String) -> Unit)? = null

    private val digits = mutableListOf<EditText>()

    /** Index of the currently focused digit view */
    private var focusPosition: Int = 0

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    /**
     * Initializes the EditText fields.
     */
    private fun init(attrs: AttributeSet?) {
        if (attrs == null) {
            error("pinLength required")
        }

        // Get the attributes
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.PinView, 0, 0)

        // Get the different customizable attributes
        val background = a.getResourceId(R.styleable.PinView_pinBackground, 0)
        val horizontalPadding = a.getDimensionPixelSize(R.styleable.PinView_pinHorizontalPadding, 0)
        val isBold = a.getBoolean(R.styleable.PinView_pinIsBold, true)
        val isFullWidth = a.getBoolean(R.styleable.PinView_isFullWidth, false)
        val margin = a.getDimensionPixelSize(
            R.styleable.PinView_pinMargin,
            resources.getDimensionPixelOffset(R.dimen.pinview_padding)
        )
        val pinWidth = a.getDimensionPixelOffset(R.styleable.PinView_pinWidth, -1)
        val textColor = a.getColor(R.styleable.PinView_pinTextColor, Color.BLACK)
        val textSize = a.getDimension(R.styleable.PinView_pinTextSize, resources.getDimension(R.dimen.pinview_text))
        val verticalPadding = a.getDimensionPixelSize(R.styleable.PinView_pinVerticalPadding, 0)
        val size = a.getInt(R.styleable.PinView_pinLength, 0)

        a.recycle()

        // Set up the LinearLayout
        orientation = HORIZONTAL

        for (i in 0 until size) {
            // Create a pin with all of its attributes
            object : AppCompatEditText(context) {

                override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
                    return CustomInputConnection(super.onCreateInputConnection(outAttrs))
                }

                inner class CustomInputConnection(target: InputConnection) :
                        InputConnectionWrapper(target, true) {

                    override fun sendKeyEvent(event: KeyEvent): Boolean {
                        if (event.action == KeyEvent.ACTION_UP &&
                                event.keyCode == KeyEvent.KEYCODE_DEL) {
                            // Back button pressed
                            if (text.toString().isNotEmpty() && focusPosition > 0) {
                                // Delete button has been clicked on a digit that can go backwards
                                val newDigit = digits[focusPosition - 1]

                                // Set the focus position to -1 to not trigger the focus listener
                                focusPosition = -1

                                // Move to the previous view and clear it
                                newDigit.text.clear()
                                newDigit.requestFocus()
                            }
                        }
                        return super.sendKeyEvent(event)
                    }
                }
            }.apply {
                val finalPinWidth = if (pinWidth != -1) pinWidth else LayoutParams.WRAP_CONTENT

                val params = LayoutParams(finalPinWidth, LayoutParams.WRAP_CONTENT, if (isFullWidth) 1f else 0f)
                params.setMargins(margin, margin, margin, margin)
                layoutParams = params
                val style = if (isBold) Typeface.BOLD else Typeface.NORMAL
                setTypeface(typeface, style)
                if (background != 0) {
                    setBackgroundResource(background)
                }
                minEms = 1
                setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
                setTextColor(textColor)
                gravity = Gravity.CENTER
                inputType = InputType.TYPE_CLASS_NUMBER
                setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
                tag = i

                // Add it to the LinearLayout and to the list of pins
                addView(this)
                digits.add(this)
            }
        }

        digits.forEach {
            // Set the FocusWatcher, the FocusChangeListener, and the InputFilters
            it.addTextChangedListener(FocusWatcher(it))
            it.onFocusChangeListener = this
            // Make sure to put our filter first, the LengthFilter will mess with our code
            it.filters = arrayOf(InputFilter.LengthFilter(1))
        }

        // Set up to catch the submission on the last digit
        digits[digits.size - 1].addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.isNotBlank()) {
                    onEntered?.invoke(digits.joinToString("") { it.text })
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
    }

    /**
     * Clears the digits and focuses on the first one
     */
    fun clear() {
        focusPosition = -1
        digits.forEach { it.text.clear() }
        digits[0].requestFocus()
    }

    override fun setEnabled(isEnabled: Boolean) {
        super.setEnabled(isEnabled)
        digits.forEach { it.isEnabled = isEnabled }
    }

    override fun onFocusChange(v: View, hasFocus: Boolean) {
        if (!hasFocus) {
            return
        }

        for (i in digits.indices) {
            // Find the digit that currently has the focus
            if (Integer.valueOf(digits[i].tag.toString()) == v.tag) {
                focusPosition = i
                break
            } else {
                // If we don't find anything, set the focus position to -1
                focusPosition = -1
            }
        }
    }

    /**
     * Automatically changes the focus from one digit to another if there is a digit after it
     *
     * @param digit View we are putting the [FocusWatcher] on
     */
    private inner class FocusWatcher(digit: EditText) : TextWatcher {

        /** Position of the view this watcher is on */
        private val position = digit.tag as Int

        override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

        override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
            // Only change if there is any text and we're not on the last pin.
            if (charSequence.toString().isNotBlank() && position < digits.size - 1) {
                digits[position + 1].requestFocus()
            }
        }

        override fun afterTextChanged(editable: Editable) {}
    }
}
