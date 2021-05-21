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
package com.nfcweblink.fragments;

import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.nfcweblink.qr.QRCallback;
import com.nfcweblink.qr.QRScanner;
import com.nfcweblink.utils.FragmentService;
import com.nfcweblink.utils.FragmentServiceInterface;
import com.nfcweblink.utils.MailBox;

public class FragQrSignTransaction extends Fragment implements FragmentServiceInterface {

    private String title = "AUTHORIZE TRANSACTION";
    private MailBox mailBox;
    private QRScanner qrScanner;

    public FragQrSignTransaction() {
        // Empty constructor required for fragment subclasses
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Setup global mailbox
        mailBox = (MailBox)getContext().getApplicationContext();

        // Change title
        getActivity().setTitle(title);

        qrScanner = new QRScanner(getActivity(), QRScanner.Mode.TRANSACTION_SIGNING);

        qrScanner.setCallback(new QRCallback() {
            @Override
            public void callbackCall(String text) {
                Log.d(this.getClass().getName(), "QRScanner callback invoked");
                callback(text);
            }
        });
        qrScanner.enable();
        return qrScanner.getView();
    }

    private void callback(String text) {
        mailBox.sendMail(MailBox.Topic.TRANSACTION_SIGNING, text);
        FragmentService.next(getActivity(), FragmentService.Page.NFC_TRANSACTION_SIGNING);
    }

    public String getTitle() {
        return title;
    }

    @Override
    public void onNfcCallback(IsoDep isoDep) {

    }
}
