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

import android.content.*;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.ViewDebug.ExportedProperty;
import android.widget.RemoteViews.RemoteView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Almost all code in this class is from the Android Open Source Project and is part of Android 4.2 (API Level 17)
 * Since I support back to Android 4.1 (API Level 16), I included this source my project for the devices without it built in.
 * All code is from AOSP and the only modifications I have made is to specify in-project XML files rather than system files.
 * @author Android Open Source Project, with slight modifications by me
 */
@RemoteView
public class TextClock extends TextView {
    /**
     * The default formatting pattern in 12-hour mode. This pattenr is used
     * if {@link #setFormat12Hour(CharSequence)} is called with a null pattern
     * or if no pattern was specified when creating an instance of this class.
     * 
     * This default pattern shows only the time, hours and minutes, and an am/pm
     * indicator.
     *
     * @see #setFormat12Hour(CharSequence)
     * @see #getFormat12Hour()
     */
    public static final CharSequence DEFAULT_FORMAT_12_HOUR = "h:mm aa";

    /**
     * The default formatting pattern in 24-hour mode. This pattenr is used
     * if {@link #setFormat24Hour(CharSequence)} is called with a null pattern
     * or if no pattern was specified when creating an instance of this class.
     *
     * This default pattern shows only the time, hours and minutes.
     * 
     * @see #setFormat24Hour(CharSequence) 
     * @see #getFormat24Hour() 
     */
    public static final CharSequence DEFAULT_FORMAT_24_HOUR = "k:mm";

    private CharSequence mFormat12 = DEFAULT_FORMAT_12_HOUR;
    private CharSequence mFormat24 = DEFAULT_FORMAT_24_HOUR;

    @ExportedProperty
    private CharSequence mFormat;
    @ExportedProperty
    private boolean mHasSeconds;

    private boolean mAttached;

    private Calendar mTime;
    private String mTimeZone;

    private final ContentObserver mFormatChangeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            chooseFormat();
            onTimeChanged();
        }

        public void onChange(boolean selfChange, Uri uri) {
            chooseFormat();
            onTimeChanged();
        }
    };

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mTimeZone == null && Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
                final String timeZone = intent.getStringExtra("time-zone");
                createTime(timeZone);
            }
            onTimeChanged();
        }
    };

    private final Runnable mTicker = new Runnable() {
        public void run() {
            onTimeChanged();
            long now = SystemClock.uptimeMillis();
            long next = now + (1000 - now % 1000);
            //getContext().sendBroadcast(new Intent("net.alamoapps.launcher.HEADER_UPDATE"));
            getHandler().postAtTime(mTicker, next);
        }
    };

    /**
     * Creates a new clock using the default patterns
     * {@link #DEFAULT_FORMAT_24_HOUR} and {@link #DEFAULT_FORMAT_12_HOUR}
     * respectively for the 24-hour and 12-hour modes.
     * 
     * @param context The Context the view is running in, through which it can
     *        access the current theme, resources, etc.
     */
    @SuppressWarnings("UnusedDeclaration")
    public TextClock(Context context) {
        super(context);
        init();
    }

    /**
     * Creates a new clock inflated from XML. This object's properties are
     * intialized from the attributes specified in XML.
     * 
     * This constructor uses a default style of 0, so the only attribute values
     * applied are those in the Context's Theme and the given AttributeSet.
     *
     * @param context The Context the view is running in, through which it can
     *        access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view
     */
    @SuppressWarnings("UnusedDeclaration")
    public TextClock(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Creates a new clock inflated from XML. This object's properties are
     * intialized from the attributes specified in XML.
     *
     * @param context The Context the view is running in, through which it can
     *        access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view
     * @param defStyle The default style to apply to this view. If 0, no style
     *        will be applied (beyond what is included in the theme). This may
     *        either be an attribute resource, whose value will be retrieved
     *        from the current theme, or an explicit style resource
     */
    public TextClock(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TextClock, defStyle, 0);
      
        try {
            CharSequence format;

            format = a.getText(R.styleable.TextClock_format12Hour);
            mFormat12 = format == null ? DEFAULT_FORMAT_12_HOUR : format;

            format = a.getText(R.styleable.TextClock_format24Hour);
            mFormat24 = format == null ? DEFAULT_FORMAT_24_HOUR : format;

            mTimeZone = a.getString(R.styleable.TextClock_timeZone);
        } finally {
            a.recycle();
        }

        init();
    }

    private void init() {
        createTime(mTimeZone);
        // Wait until onAttachedToWindow() to handle the ticker
        chooseFormat(false);
    }

    private void createTime(String timeZone) {
        if (timeZone != null) {
            mTime = Calendar.getInstance(TimeZone.getTimeZone(timeZone));
        } else {
            mTime = Calendar.getInstance();
        }
    }

    /**
     * Returns the formatting pattern used to display the date and/or time
     * in 12-hour mode. The formatting pattern syntax is described in
     * {@link DateFormat}.
     * 
     * @return A {@link CharSequence} or null.
     * 
     * @see #setFormat12Hour(CharSequence) 
     * @see #is24HourModeEnabled() 
     */
    @ExportedProperty
    public CharSequence getFormat12Hour() {
        return mFormat12;
    }

    /**
     * Specifies the formatting pattern used to display the date and/or time
     * in 12-hour mode. The formatting pattern syntax is described in
     * {@link DateFormat}.
     *
     * If this pattern is set to null, {@link #getFormat24Hour()} will be used
     * even in 12-hour mode. If both 24-hour and 12-hour formatting patterns
     * are set to null, {@link #DEFAULT_FORMAT_24_HOUR} and
     * {@link #DEFAULT_FORMAT_12_HOUR} will be used instead.
     *
     * @param format A date/time formatting pattern as described in {@link DateFormat}
     * 
     * @see #getFormat12Hour()
     * @see #is24HourModeEnabled()
     * @see #DEFAULT_FORMAT_12_HOUR
     * @see DateFormat
     * 
     * @attr ref android.R.styleable#TextClock_format12Hour
     */
    //@RemotableViewMethod
    public void setFormat12Hour(CharSequence format) {
        mFormat12 = format;

        chooseFormat();
        onTimeChanged();
    }
    
    public void setTypeface(Typeface tf){
    	if(tf!=null)super.setTypeface(tf);
    }

    /**
     * Returns the formatting pattern used to display the date and/or time
     * in 24-hour mode. The formatting pattern syntax is described in
     * {@link DateFormat}.
     *
     * @return A {@link CharSequence} or null.
     *
     * @see #setFormat24Hour(CharSequence)
     * @see #is24HourModeEnabled()
     */
    @ExportedProperty
    public CharSequence getFormat24Hour() {
        return mFormat24;
    }

    /**
     * Specifies the formatting pattern used to display the date and/or time
     * in 24-hour mode. The formatting pattern syntax is described in
     * {@link DateFormat}.
     * 
     * If this pattern is set to null, {@link #getFormat12Hour()} will be used
     * even in 24-hour mode. If both 24-hour and 12-hour formatting patterns
     * are set to null, {@link #DEFAULT_FORMAT_24_HOUR} and
     * {@link #DEFAULT_FORMAT_12_HOUR} will be used instead.
     *
     * @param format A date/time formatting pattern as described in {@link DateFormat}
     *
     * @see #getFormat24Hour()
     * @see #is24HourModeEnabled() 
     * @see #DEFAULT_FORMAT_24_HOUR
     * @see DateFormat
     *
     * @attr ref android.R.styleable#TextClock_format24Hour
     */
    //@RemotableViewMethod
    public void setFormat24Hour(CharSequence format) {
        mFormat24 = format;

        chooseFormat();
        onTimeChanged();
    }

    /**
     * Indicates whether the system is currently using the 24-hour mode.
     * 
     * When the system is in 24-hour mode, this view will use the pattern
     * returned by {@link #getFormat24Hour()}. In 12-hour mode, the pattern
     * returned by {@link #getFormat12Hour()} is used instead.
     * 
     * If either one of the formats is null, the other format is used. If
     * both formats are null, the default values {@link #DEFAULT_FORMAT_12_HOUR}
     * and {@link #DEFAULT_FORMAT_24_HOUR} are used instead.
     * 
     * @return true if time should be displayed in 24-hour format, false if it
     *         should be displayed in 12-hour format.
     * 
     * @see #setFormat12Hour(CharSequence)
     * @see #getFormat12Hour() 
     * @see #setFormat24Hour(CharSequence)
     * @see #getFormat24Hour() 
     */
    public boolean is24HourModeEnabled() {
        return DateFormat.is24HourFormat(getContext());
    }

    /**
     * Indicates which time zone is currently used by this view.
     * 
     * @return The ID of the current time zone or null if the default time zone,
     *         as set by the user, must be used
     *
     * @see TimeZone
     * @see java.util.TimeZone#getAvailableIDs()
     * @see #setTimeZone(String) 
     */
    public String getTimeZone() {
        return mTimeZone;
    }

    /**
     * Sets the specified time zone to use in this clock. When the time zone
     * is set through this method, system time zone changes (when the user
     * sets the time zone in settings for instance) will be ignored.
     *
     * @param timeZone The desired time zone's ID as specified in {@link TimeZone}
     *                 or null to user the time zone specified by the user
     *                 (system time zone)
     *
     * @see #getTimeZone()
     * @see java.util.TimeZone#getAvailableIDs()
     * @see TimeZone#getTimeZone(String)
     *
     * @attr ref android.R.styleable#TextClock_timeZone
     */
    //@RemotableViewMethod
    public void setTimeZone(String timeZone) {
        mTimeZone = timeZone;

        createTime(timeZone);
        onTimeChanged();
    }

    /**
     * Selects either one of {@link #getFormat12Hour()} or {@link #getFormat24Hour()}
     * depending on whether the user has selected 24-hour format.
     * 
     * Calling this method does not schedule or unschedule the time ticker.
     */
    private void chooseFormat() {
        chooseFormat(true);
    }

    /**
     * Selects either one of {@link #getFormat12Hour()} or {@link #getFormat24Hour()}
     * depending on whether the user has selected 24-hour format.
     * 
     * @param handleTicker true if calling this method should schedule/unschedule the
     *                     time ticker, false otherwise
     */
    private void chooseFormat(boolean handleTicker) {
        final boolean format24Requested = is24HourModeEnabled();

        if (format24Requested) {
            mFormat = abc(mFormat24, mFormat12, DEFAULT_FORMAT_24_HOUR);
        } else {
            mFormat = abc(mFormat12, mFormat24, DEFAULT_FORMAT_12_HOUR);
        }

        boolean hadSeconds = mHasSeconds;
        mHasSeconds = hasSeconds(mFormat);

        if (handleTicker && mAttached && hadSeconds != mHasSeconds) {
            if (hadSeconds) getHandler().removeCallbacks(mTicker);
            else mTicker.run();
        }
    }
    public static boolean hasSeconds(CharSequence inFormat) {
        if (inFormat == null) return false;

        final int length = inFormat.length();

        int c;
        int count;

        for (int i = 0; i < length; i += count) {
            count = 1;
            c = inFormat.charAt(i);

            if (c == '\'') {
                count = skipQuotedText(inFormat, i, length);
            } else if (c == 's') {
                return true;
            }
        }

        return false;
    }

    private static int skipQuotedText(CharSequence s, int i, int len) {
    	char QUOTE = '\'';
        if (i + 1 < len && s.charAt(i + 1) == QUOTE) {
            return 2;
        }

        int count = 1;
        // skip leading quote
        i++;

        while (i < len) {
            char c = s.charAt(i);

            if (c == QUOTE) {
                count++;
                //  QUOTEQUOTE -> QUOTE
                if (i + 1 < len && s.charAt(i + 1) == QUOTE) {
                    i++;
                } else {
                    break;
                }
            } else {
                i++;
                count++;
            }
        }

        return count;
    }

    /**
     * Returns a if not null, else return b if not null, else return c.
     */
    private static CharSequence abc(CharSequence a, CharSequence b, CharSequence c) {
        return a == null ? (b == null ? c : b) : a;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mAttached) {
            mAttached = true;

            registerReceiver();
            registerObserver();

            createTime(mTimeZone);

            if (mHasSeconds) {
                mTicker.run();
            } else {
                onTimeChanged();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mAttached) {
            unregisterReceiver();
            unregisterObserver();

            getHandler().removeCallbacks(mTicker);

            mAttached = false;
        }
    }

    private void registerReceiver() {
        final IntentFilter filter = new IntentFilter();

        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);

        getContext().registerReceiver(mIntentReceiver, filter, null, getHandler());
    }

    private void registerObserver() {
        final ContentResolver resolver = getContext().getContentResolver();
        resolver.registerContentObserver(Settings.System.CONTENT_URI, true, mFormatChangeObserver);
    }

    private void unregisterReceiver() {
        getContext().unregisterReceiver(mIntentReceiver);
    }

    private void unregisterObserver() {
        final ContentResolver resolver = getContext().getContentResolver();
        resolver.unregisterContentObserver(mFormatChangeObserver);
    }

    private void onTimeChanged() {
        mTime.setTimeInMillis(System.currentTimeMillis());
        setText(DateFormat.format(mFormat, mTime));
    }
}