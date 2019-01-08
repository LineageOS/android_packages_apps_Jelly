/*
 * Copyright (C) 2017 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lineageos.jelly.ui;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.lineageos.jelly.R;

public class KeyValueView extends LinearLayout {
    private TextView mKeyView;
    private TextView mValueView;

    public KeyValueView(Context context) {
        super(context);
        init();
    }

    public KeyValueView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public KeyValueView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.key_value_view, this);

        mKeyView = findViewById(R.id.key);
        mValueView = findViewById(R.id.value);
    }

    public void setText(@StringRes int attributeTextResId, String value) {
        if (!value.isEmpty()) {
            this.mKeyView.setText(attributeTextResId);
            this.mValueView.setText(value);
        } else {
            setVisibility(View.GONE);
        }
    }
}
