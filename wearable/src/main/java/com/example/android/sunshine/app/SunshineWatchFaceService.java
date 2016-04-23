/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.sunshine.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Sunshine Digital watch face. On devices with low-bit ambient mode,
 * the text is drawn without anti-aliasing in ambient mode.
 */
public class SunshineWatchFaceService extends CanvasWatchFaceService {

    private static final String SANS_SERIF_CONDENSED = "sans-serif-condensed";

    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    private static final Typeface CONDENSED_TYPEFACE =
            Typeface.create(SANS_SERIF_CONDENSED, Typeface.NORMAL);

    private static final Typeface BOLD_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    private static final int MSG_LOAD_WEATHER = 0;
    public static final String ACTION_WEATHER_CHANGED = "com.geaden.android.sunshine.wearable.ACTION_WEATHER_CHANGED";

    public static final String EXTRA_HI = "extra_hi";
    public static final String EXTRA_LO = "extra_lo";
    public static final String EXTRA_ART = "extra_art";

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<SunshineWatchFaceService.Engine> mWeakReference;

        public EngineHandler(SunshineWatchFaceService.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            SunshineWatchFaceService.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        final CharSequence DATE_FORMAT = "EEE, MMM d yyyy";
        final Handler mUpdateTimeHandler = new EngineHandler(this);

        static final String COLON_STRING = ":";

        boolean mRegisteredTimeZoneReceiver = false;
        boolean mRegisteredWeatherChangeReceiver = false;

        Paint mBackgroundPaint;
        Paint mHourPaint;
        Paint mColonPaint;
        Paint mMinutePaint;
        Paint mDatePaint;
        Paint mLinePaint;
        Paint mHiPaint;
        Paint mLoPaint;
        float mColonWidth;

        // Actual data
        float mLoTemp;
        float mHiTemp;
        Bitmap mBitmap;

        boolean mAmbient;
        boolean mBurnInProtection;

        /**
         * Handles time zone and locale changes.
         */
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };

        /**
         * Handles weather changes.
         */
        final BroadcastReceiver mWeatherChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("mWeatherChangeReceiver", intent.getAction());
                if (intent.getAction().equals(ACTION_WEATHER_CHANGED)) {
                    Log.d("mWeatherChangeReceiver", "Weather Changed");
                    mHiTemp = intent.getFloatExtra(EXTRA_HI, 0f);
                    mLoTemp = intent.getFloatExtra(EXTRA_LO, 0f);
                    byte[] bytes = intent.getByteArrayExtra(EXTRA_ART);
                    mBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    // Display results.
                    invalidate();
                }
            }
        };

        float mXOffset;
        float mYOffset;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        Calendar mCalendar;
        Date mDate;

        boolean mShouldDrawColons;
        float mLineHeight;
        float mDecorLineLength;
        float mArtSize;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());
            Resources resources = SunshineWatchFaceService.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(ContextCompat.getColor(SunshineWatchFaceService.this,
                    R.color.background));

            mLineHeight = resources.getDimension(R.dimen.digital_line_height);
            mArtSize = resources.getDimension(R.dimen.digital_art_size);

            // Hours text paint.
            mHourPaint = createTextPaint(ContextCompat.getColor(SunshineWatchFaceService.this,
                    R.color.digital_text), BOLD_TYPEFACE);

            // Colon text paint.
            mColonPaint = createTextPaint(ContextCompat.getColor(SunshineWatchFaceService.this,
                    R.color.digital_text), BOLD_TYPEFACE);

            // Minutes text paint.
            mMinutePaint = createTextPaint(ContextCompat.getColor(SunshineWatchFaceService.this, R.color.digital_text));

            // Date text paint.
            mDatePaint = createTextPaint(ContextCompat.getColor(SunshineWatchFaceService.this, R.color.digital_text),
                    CONDENSED_TYPEFACE);

            // Line paint.
            mLinePaint = createTextPaint(ContextCompat.getColor(SunshineWatchFaceService.this,
                    R.color.digital_text));

            // Hi text paint.
            mHiPaint = createTextPaint(ContextCompat.getColor(SunshineWatchFaceService.this, R.color.digital_text), BOLD_TYPEFACE);

            // Lo text paint.
            mLoPaint = createTextPaint(ContextCompat.getColor(SunshineWatchFaceService.this, R.color.digital_text));

            mCalendar = Calendar.getInstance();
            mDate = new Date();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        /**
         * Creates text paint with normal typeface.
         *
         * @param defaultInteractiveColor the default color of text.
         * @return text paint with normal typeface.
         */
        private Paint createTextPaint(int defaultInteractiveColor) {
            return createTextPaint(defaultInteractiveColor, NORMAL_TYPEFACE);
        }

        /**
         * Creates text paint with provided color and typeface.
         *
         * @param defaultInteractiveColor the text color.
         * @param typeface                the typeface.
         * @return text paint with given color and typeface.
         */
        private Paint createTextPaint(int defaultInteractiveColor, Typeface typeface) {
            Paint paint = new Paint();
            paint.setColor(defaultInteractiveColor);
            paint.setTypeface(typeface);
            paint.setAntiAlias(true);
            return paint;
        }


        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                // Update time zone in case it changed while we weren't visible.
                // Update time zone and date formats, in case they changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                mRegisteredTimeZoneReceiver = true;
                IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
                SunshineWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
            }
            if (!mRegisteredWeatherChangeReceiver) {
                mRegisteredWeatherChangeReceiver = true;
                IntentFilter weatherFilter = new IntentFilter(ACTION_WEATHER_CHANGED);
                LocalBroadcastManager.getInstance(SunshineWatchFaceService.this)
                        .registerReceiver(mWeatherChangeReceiver, weatherFilter);
            }
        }

        private void unregisterReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                mRegisteredTimeZoneReceiver = false;
                SunshineWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
            }
            if (mRegisteredWeatherChangeReceiver) {
                mRegisteredWeatherChangeReceiver = false;
                LocalBroadcastManager.getInstance(SunshineWatchFaceService.this)
                        .unregisterReceiver(mWeatherChangeReceiver);
            }
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = SunshineWatchFaceService.this.getResources();

            mXOffset = resources.getDimension(R.dimen.digital_x_offset);

            float textSize = resources.getDimension(R.dimen.digital_text_size);

            // Text size for time.
            float timeTextSize = resources.getDimension(R.dimen.digital_time_text_size);
            // Text size for date.
            float dateTextSize = resources.getDimension(R.dimen.digital_date_text_size);
            // Text size for temperature.
            float tempTextSize = resources.getDimension(R.dimen.digital_temp_text_size);
            // Decoration line length.
            mDecorLineLength = resources.getDimension(R.dimen.decor_line_length);

            mHourPaint.setTextSize(timeTextSize);
            mMinutePaint.setTextSize(timeTextSize);
            mDatePaint.setTextSize(dateTextSize);
            mHiPaint.setTextSize(tempTextSize);
            mLoPaint.setTextSize(tempTextSize);
            mColonPaint.setTextSize(timeTextSize);

            mColonWidth = mColonPaint.measureText(COLON_STRING);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION,
                    false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    for (Paint paint : new Paint[]{
                            mHourPaint,
                            mColonPaint,
                            mMinutePaint,
                            mDatePaint,
                            mLinePaint,
                            mHiPaint,
                            mLoPaint
                    }) {
                        paint.setAntiAlias(!inAmbientMode);
                    }
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            Resources resources = SunshineWatchFaceService.this.getResources();
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    break;
            }
            invalidate();
        }

        /**
         * Gets coordinate of the start of the line from center.
         *
         * @param bounds      current bounds.
         * @param totalLength total string length to be drawn.
         * @return start of the line.
         */
        private float getStartOfLine(Rect bounds, float totalLength) {
            return bounds.centerX() - totalLength / 2;
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
            }

            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);
            mDate.setTime(now);
            // Show colons for the first half of each second so the colons blink on when the time
            // updates.
            mShouldDrawColons = (System.currentTimeMillis() % 1000) < 500;

            String hourString = formatTwoDigitNumber(mCalendar.get(Calendar.HOUR_OF_DAY));
            String minuteString = formatTwoDigitNumber(mCalendar.get(Calendar.MINUTE));

            // Beginning of the time line.
            float x = getStartOfLine(bounds, (mHourPaint.measureText(hourString) + mColonWidth +
                    mMinutePaint.measureText(minuteString)));

            float y = mYOffset;

            canvas.drawText(hourString, x, y, mHourPaint);
            x += mHourPaint.measureText(hourString);

            // In ambient mode always draw the first colon. Otherwise, draw the
            // first colon for the first half of each second.
            if (isInAmbientMode() || mShouldDrawColons) {
                canvas.drawText(COLON_STRING, x, y, mHourPaint);
            }

            x += mColonWidth;

            // Draw the minutes.
            canvas.drawText(minuteString, x, y, mMinutePaint);

            // Date
            String dateString = DateFormat.format(DATE_FORMAT, mDate).toString().toUpperCase();

            // Beginning of the date.
            x = getStartOfLine(bounds, mDatePaint.measureText(dateString));

            y += mLineHeight;

            canvas.drawText(dateString, x, y, mDatePaint);

            // Draw a line
            if (!isInAmbientMode()) {
                x = getStartOfLine(bounds, mDecorLineLength);
                y += mLineHeight;
                canvas.drawLine(x, y, x + mDecorLineLength, y, mLinePaint);
            }

            // Temperature
            float tempX = 0;

            y += 0.5f * mLineHeight;

            String hiTempString = getString(R.string.format_temperature, mHiTemp);
            String loTempString = getString(R.string.format_temperature, mLoTemp);

            if (!isInAmbientMode() && null != mBitmap && getPeekCardPosition().isEmpty()) {
                tempX = getStartOfLine(bounds, mArtSize + mHiPaint.measureText(hiTempString)
                        + mLoPaint.measureText(loTempString));
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                int width = mBitmap.getWidth();
                int height = mBitmap.getHeight();
                float scale = mArtSize / width;
                Matrix matrix = new Matrix();
                // resize the bit map
                matrix.postScale(scale, scale);
                // recreate the new Bitmap
                Bitmap resizedArt = Bitmap.createBitmap(mBitmap, 0, 0, width, height, matrix, false);
                canvas.drawBitmap(resizedArt, tempX, y, mLinePaint);
                tempX += resizedArt.getWidth();
            } else {
                tempX = getStartOfLine(bounds, mHiPaint.measureText(hiTempString)
                        + mLoPaint.measureText(hiTempString));
            }

            // Only render the temperature if there is no peek card, so they do not bleed
            // into each other in ambient mode.
            if (getPeekCardPosition().isEmpty()) {
                y += mLineHeight;
                canvas.drawText(hiTempString, tempX, y, mHiPaint);
                tempX += mHiPaint.measureText(hiTempString);
                canvas.drawText(loTempString, tempX, y, mLoPaint);
            }
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        /**
         * Heler method to format two digit number.
         *
         * @param hour the hour.
         * @return formatted hour.
         */
        private String formatTwoDigitNumber(int hour) {
            return String.format("%02d", hour);
        }
    }
}
