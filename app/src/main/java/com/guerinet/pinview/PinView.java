/*
 * Copyright 2017 Julien Guerinet
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

package com.guerinet.pinview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;


/**
 * Allows the user to enter a decimal pin code
 * @author Julien Guerinet
 * @since 1.0.0
 */
public class PinView extends LinearLayout implements View.OnFocusChangeListener {

    private final List<EditText> digits;

    /**
     * Index of the currently focused digit view
     */
    private int focusPosition;

    @Nullable
    private PinReceiver receiver;

    /**
     * Default programmatic constructor
     *
     * @param context App context
     * @param size    Number of digits needed, should be more than 1
     */
    public PinView(Context context, int size) {
        super(context);
        digits = new ArrayList<>();
        init(size);
    }

    public PinView(Context context) {
        super(context);
        digits = new ArrayList<>();
        init(0);
    }

    public PinView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        digits = new ArrayList<>();
        init(getSize(context, attrs));
    }

    public PinView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        digits = new ArrayList<>();
        init(getSize(context, attrs));
    }

    /**
     * Obtains the pin size from the XML declaration
     *
     * @param context App context
     * @param attrs   Nullable {@link AttributeSet}
     * @return Size retrieved from the attributes, 0 if none found
     */
    private int getSize(Context context, @Nullable AttributeSet attrs) {
        int size = 0;

        if (attrs == null) {
            return size;
        }

        // Get the attributes
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.PinView, 0, 0);

        try {
            // Get the size attribute
            size = a.getInt(R.styleable.PinView_pinLength, 0);
        } finally {
            a.recycle();
        }
        return size;
    }

    /**
     * Initialize EditText fields.
     */
    private void init(final int size) {
        // Set up the LinearLayout
        setOrientation(HORIZONTAL);
        setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // Get the needed resources
        int medium = getResources().getDimensionPixelOffset(R.dimen.pinview_padding);
        float headline = getResources().getDimension(R.dimen.pinview_text);

        for (int i = 0; i < size; i ++) {
            // Create a pin with all of its attributes
            EditText pin = new AppCompatEditText(getContext()) {
                @Override
                public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
                    return new CustomInputConnection(super.onCreateInputConnection(outAttrs));
                }

                class CustomInputConnection extends InputConnectionWrapper {

                    CustomInputConnection(InputConnection target) {
                        super(target, true);
                    }

                    @Override
                    public boolean sendKeyEvent(KeyEvent event) {
                        if (event.getAction() == KeyEvent.ACTION_UP
                                && event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                            // Back button pressed
                            if (TextUtils.isEmpty(getText().toString()) && focusPosition > 0) {
                                // Delete button has been clicked on a digit that can go backwards
                                EditText newDigit = digits.get(focusPosition - 1);

                                // Set the focus position to -1 to not trigger the focus listener
                                focusPosition = -1;

                                // Move to the previous view and clear it
                                newDigit.getText().clear();
                                newDigit.requestFocus();
                            }
                        }
                        return super.sendKeyEvent(event);
                    }
                }
            };
            LayoutParams params = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1);
            params.setMargins(medium, medium, medium, medium);
            pin.setLayoutParams(params);
            pin.setTypeface(pin.getTypeface(), Typeface.BOLD);
            pin.setGravity(Gravity.CENTER);
            pin.setInputType(InputType.TYPE_CLASS_NUMBER);
            pin.setTextSize(TypedValue.COMPLEX_UNIT_PX, headline);
            pin.setTag(i);

            // Add it to the LinearLayout and to the list of pins
            addView(pin);
            digits.add(pin);
        }

        for (EditText digit : digits) {
            // Set the FocusWatcher, the FocusChangeListener, and the InputFilters
            digit.addTextChangedListener(new FocusWatcher(digit));
            digit.setOnFocusChangeListener(this);
            // Make sure to put our filter first, the LengthFilter will mess with our code
            digit.setFilters(new InputFilter[] {new InputFilter.LengthFilter(1)});
        }

        // Set up to catch the submission on the last digit
        digits.get(digits.size() - 1).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!TextUtils.isEmpty(s) && receiver != null) {
                    StringBuilder builder = new StringBuilder(size);

                    // Concatenate all of the view info
                    for (EditText digit : digits) {
                        builder.append(digit.getText().toString());
                    }

                    receiver.onEntered(builder.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * @param receiver {@link PinReceiver} to use for this view
     */
    public void setPinReceiver(PinReceiver receiver) {
        this.receiver = receiver;
    }

    /**
     * Clears the digits and focuses on the first one
     */
    public void clear() {
        focusPosition = -1;
        for (EditText digit : digits) {
            digit.getText().clear();
        }
        digits.get(0).requestFocus();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        for (EditText digit : digits) {
            digit.setEnabled(enabled);
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
            return;
        }

        for (int i = 0; i < digits.size(); i ++) {
            // Find the digit that currently has the focus
            if (Integer.valueOf(digits.get(i).getTag().toString()).equals(v.getTag())) {
                focusPosition = i;
                break;
            } else {
                // If we don't find anything, set the focus position to -1
                focusPosition = -1;
            }
        }
    }

    /**
     * Watches the entered pin
     */
    public interface PinReceiver {

        /**
         * Called when the user has finished entering the pin
         *
         * @param pin Entered pin
         */
        void onEntered(String pin);
    }

    /**
     * Automatically changes the focus from one digit to another if there is a digit after it
     */
    private class FocusWatcher implements TextWatcher {

        /**
         * Position of the view this watcher is on
         */
        private final int position;

        /**
         * Default Constructor
         *
         * @param digit View that we are currently putting the FocusWatcher on
         */
        private FocusWatcher(EditText digit) {
            this.position = (int) digit.getTag();
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            // Only change if there is any text and we're not on the last pin.
            if (!TextUtils.isEmpty(charSequence.toString()) && position < digits.size() - 1) {
                digits.get(position + 1).requestFocus();
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {}
    }
}
