/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2022, Tikal Knowledge, Ltd.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * • Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * • Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * • Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.tikalk.worktracker.time;

import static java.lang.Math.abs;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.core.view.GestureDetectorCompat;

// FIXME This class is in Java, and not Kotlin, because `MotionEvent e1` *does* receive `null` value.
public class TimeListSwipeDay {

    private final boolean isLocaleRTL;
    private final OnSwipeListener listener;
    private final GestureDetectorCompat gestureDetector;

    public TimeListSwipeDay(Context context, OnSwipeListener listener) {
        String language = context.getResources().getConfiguration().locale.getLanguage();
        this.isLocaleRTL = isLocaleRTL(language);
        this.listener = listener;

        final GestureDetector.OnGestureListener gestureListener =
            new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onFling(
                    @NonNull MotionEvent e1,
                    @NonNull MotionEvent e2,
                    float velocityX,
                    float velocityY
                ) {
                    float vx = abs(velocityX);
                    float vy = abs(velocityY);
                    if ((vx > vy) && (vx > 500)) {
                        // Fling from right to left?
                        if (velocityX < 0) {
                            if (isLocaleRTL) {
                                notifyPreviousDay();
                            } else {
                                notifyNextDay();
                            }
                        } else {
                            if (isLocaleRTL) {
                                notifyNextDay();
                            } else {
                                notifyPreviousDay();
                            }
                        }
                        return true;
                    }
                    return super.onFling(e1, e2, velocityX, velocityY);
                }
            };

        gestureDetector = new GestureDetectorCompat(context, gestureListener);
    }

    private boolean isLocaleRTL(@NonNull String language) {
        return language.equals("iw") || language.equals("he") || language.equals("ar");
    }

    private void notifyNextDay() {
        listener.onSwipeNextDay();
    }

    private void notifyPreviousDay() {
        listener.onSwipePreviousDay();
    }

    public boolean onTouchEvent(@NonNull MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    interface OnSwipeListener {
        void onSwipePreviousDay();

        void onSwipeNextDay();
    }
}
