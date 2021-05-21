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

import android.nfc.tech.IsoDep;
import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.nfcweblink.R;
import com.nfcweblink.fragments.FragNfcSignTransaction;
import com.nfcweblink.fragments.FragQrReadAddress;
import com.nfcweblink.fragments.FragHome;
import com.nfcweblink.fragments.FragNfcReadAddress;
import com.nfcweblink.fragments.FragQrSignTransaction;

import java.util.List;

public class FragmentService implements FragmentManager.OnBackStackChangedListener {

    public enum Page {
        HOME,
        QR_READ_ADDRESS,
        NFC_READ_ADDRESS,
        QR_TRANSACTION_SIGNING,
        NFC_TRANSACTION_SIGNING,
    }

    private FragmentManager fragmentManager;

    public FragmentService(FragmentActivity fa) {
        FragmentManager fm = fa.getSupportFragmentManager();
        fm.addOnBackStackChangedListener(this);
        fragmentManager = fm;
    }

    /**
     * To call current fragment's nfcCallback()
     */
    public void nfcCallback(IsoDep isoDep) {

        List<Fragment> fs = fragmentManager.getFragments();
        Log.d(this.getClass().getName() , "onBackStackChanged: Number of fragments in stack" + fs.size());
        if (fs.size() > 0) { // no callback for HOME fragment
            Fragment f = fs.get(fs.size() - 1);
            ((FragmentServiceInterface)f).onNfcCallback(isoDep);
        }

    }

    /**
     * Internal use to update title
     */
    @Override
    public void onBackStackChanged() {

        List<Fragment> fs = fragmentManager.getFragments();
        Log.d(this.getClass().getName() , "onBackStackChanged: Number of fragments in stack" + fs.size());
        if (fs.size() > 0) { // no point to pop HOME fragment
            Fragment f = fs.get(fs.size() - 1);
            String title = ((FragmentServiceInterface)f).getTitle();
            f.getActivity().setTitle(title);
        }

    }

    /**
     * Start clean with empty back stack,
     * outcome same as reset()
     * @param fa
     */
    public static void start(FragmentActivity fa) {
        process(fa, Page.HOME, false);
    }

    /**
     * Remember the old fragment before moving on
     * @param fa
     * @param page
     */
    public static void next(FragmentActivity fa, Page page) {
        process(fa, page, false);
    }

    /**
     * Return to the previous fragment and update the title
     * @param fa
     */
    public static void back(FragmentActivity fa) {
        FragmentManager fm = fa.getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 1) {
            fm.popBackStack();
        }
    }

    /**
     * Reset to home page and clear all back stack,
     * outcome same as start()
     * @param fa
     */
    public static void reset(FragmentActivity fa) {
        process(fa, Page.HOME, true);
    }

    private static void process(FragmentActivity fa, Page page, boolean toClean) {
        // update the main content by replacing fragments
        Fragment fragment;

        switch (page) {
            case QR_READ_ADDRESS:
                fragment = new FragQrReadAddress();
                break;
            case NFC_READ_ADDRESS:
                fragment = new FragNfcReadAddress();
                break;
            case QR_TRANSACTION_SIGNING:
                fragment = new FragQrSignTransaction();
                break;
            case NFC_TRANSACTION_SIGNING:
                fragment = new FragNfcSignTransaction();
                break;
            case HOME:
            default:
                fragment = new FragHome();
                break;
        }

        if (toClean) {
            clearStack(fa.getSupportFragmentManager());
        } else {
            FragmentManager fm = fa.getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            ft.add(R.id.frame_fragment, fragment, "");
            ft.addToBackStack(null);
            ft.commit();
        }
    }

    private static void clearStack(FragmentManager fm) {
        for (int i = 0; i < fm.getBackStackEntryCount() - 1; i++) {
            fm.popBackStack();
        }
    }

}
