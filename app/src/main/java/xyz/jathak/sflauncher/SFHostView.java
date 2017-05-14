/*
 * Copyright (C) 2009 The Android Open Source Project
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

package xyz.jathak.sflauncher;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.os.Parcelable;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

public class SFHostView extends AppWidgetHostView {

    private static final String TAG = "SFHostView";

    private boolean mHasPerformedLongPress;

    private LayoutInflater mInflater;

    public SFHostView(Context context) {
        super(context.getApplicationContext());
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    protected View getErrorView() {
        View v = mInflater.inflate(R.layout.titletext, this, false);
        ((TextView)v.findViewById(R.id.textView)).setText("Error!");
        return v;
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        try {
            super.dispatchRestoreInstanceState(container);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return false;
    }

}
