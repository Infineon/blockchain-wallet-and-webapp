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
import android.widget.Button;

import androidx.fragment.app.Fragment;

import com.nfcweblink.R;
import com.nfcweblink.utils.FragmentService;
import com.nfcweblink.utils.FragmentServiceInterface;
import com.nfcweblink.utils.MailBox;

public class FragHome extends Fragment implements FragmentServiceInterface {

    public String title = "HOME";
    private MailBox mailBox;

    public FragHome() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        // Setup global mailbox
        mailBox = (MailBox)getContext().getApplicationContext();

        // Change title
        getActivity().setTitle(title);

        // Setup listener 1
        final Button btn_reg = rootView.findViewById(R.id.btn_reg);
        btn_reg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(this.getClass().getName(), "Button (" + btn_reg.getText().toString() + ") onClick invoked");
                FragmentService.next(getActivity(), FragmentService.Page.QR_READ_ADDRESS);
            }
        });

        // Setup listener 2
        final Button btn_sign = rootView.findViewById(R.id.btn_sign);
        btn_sign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(this.getClass().getName(), "Button (" + btn_sign.getText().toString() + ") onClick invoked");
                FragmentService.next(getActivity(), FragmentService.Page.QR_TRANSACTION_SIGNING);
            }
        });

        return rootView;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public void onNfcCallback(IsoDep isoDep) {

    }
}
