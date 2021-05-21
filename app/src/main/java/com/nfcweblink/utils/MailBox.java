/**
 * MIT License
 *
 * Copyright (c) 2021 Infineon Technologies AG
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE
 */
package com.nfcweblink.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

public class MailBox extends Application {

    private Activity activity;

    public enum Topic {
        ADDRESS_REGISTRATION,
        TRANSACTION_SIGNING,
    };

    public void init(Activity activity) {
        this.activity = activity;
    }

    /* Read and keep */
    public String readMail(Topic topic) {
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        return sharedPref.getString(topic.toString(), null);
    }

    /* Read and dispose */
    public String receiveMail(Topic topic) {
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        String message = sharedPref.getString(topic.toString(), null);

        if (message != null) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.remove(topic.toString());
            editor.commit();
        }

        return message;
    }

    /* Remove specific */
    public void removeMail(Topic topic) {
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        String message = sharedPref.getString(topic.toString(), null);

        if (message != null) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.remove(topic.toString());
            editor.commit();
        }
    }

    /* Send */
    public boolean sendMail(Topic topic, String message) {
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(topic.toString(), message);
        editor.commit();

        return true;
    }

    /* Remove all */
    public void removeMails() {
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        editor.commit();
    }

}