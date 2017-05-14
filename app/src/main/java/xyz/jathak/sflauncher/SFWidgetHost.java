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

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;

/**
 * This class is mostly adapted from open source code that is part of the stock homescreen in the Android Open Source Project.
 * It has since been modified by the CyanogenMod Team for the Trebuchet launcher.
 * I modified this slightly to meet my needs.
 * @author The Android Open Source Project, modified by the CyanogenMod Team and later by Jack Thakar
 */
public class SFWidgetHost extends AppWidgetHost {
    public SFWidgetHost(Context context, int hostId) {
        super(context, hostId);
    }

    @Override
    protected AppWidgetHostView onCreateView(Context context, int appWidgetId,
            AppWidgetProviderInfo appWidget) {
        return new SFHostView(context);
    }

    @Override
    public void stopListening() {
        super.stopListening();
        clearViews();
    }
}
