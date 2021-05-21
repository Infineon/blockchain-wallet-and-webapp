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
package com.nfcweblink.qr;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.dlazaro66.qrcodereaderview.QRCodeReaderView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nfcweblink.R;
import com.nfcweblink.models.AddressRegistrationReq;
import com.nfcweblink.models.TransactionSigningReq;
import com.nfcweblink.utils.MailBox;

public class QRScanner implements QRCodeReaderView.OnQRCodeReadListener{

    public enum Mode {
        ADDRESS_REGISTRATION,
        TRANSACTION_SIGNING,
    };

    public static final int MY_PERMISSIONS_REQUEST_CAMERA = 0;

    private QRCallback callback;
    private Mode qrMode;
    private QRCodeReaderView qrCodeReaderView;
    private TextView textView;
    private View rootView;
    private MailBox mailBox;
    private Context context;

    public QRScanner(Activity activity, Mode mode) {
        callback = null;
        context = activity;
        qrMode = mode;
        mailBox = (MailBox)((Context)activity).getApplicationContext();

        rootView = activity.getLayoutInflater().inflate(R.layout.qr_scanner,  (ViewGroup)activity.getWindow().getDecorView().getRootView(), false);
        qrCodeReaderView = rootView.findViewById(R.id.qr_decoderview);
        textView = rootView.findViewById(R.id.qr_textview);

        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_CAMERA);

        } else {
            getQR();
        }

        Animation anim = new AlphaAnimation(0.4f, 1.0f);
        anim.setDuration(500); //You can manage the blinking time with this parameter
        anim.setStartOffset(20);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(Animation.INFINITE);
        textView.startAnimation(anim);

        switch(qrMode) {
            case ADDRESS_REGISTRATION:
                textView.setText("Scanning QR for Address Registration...");
                break;
            case TRANSACTION_SIGNING:
                textView.setText("Scanning QR for Transaction info...");
                break;
            default:
                activity.setTitle("ERROR");
                textView.setText("ERROR");
                break;
        }

    }

    public void setCallback(QRCallback callback) {
        this.callback = callback;
    }

    public void enable() {
        qrCodeReaderView.setQRDecodingEnabled(true);
    }

    public void disable() {
        qrCodeReaderView.setQRDecodingEnabled(false);
    }

    public View getView() {
        return rootView;
    }

    private void getQR() {
        try {
            qrCodeReaderView.setOnQRCodeReadListener(this);

            // Use this function to enable/disable decoding
            qrCodeReaderView.setQRDecodingEnabled(false);

            // Use this function to change the autofocus interval (default is 5 secs)
            qrCodeReaderView.setAutofocusInterval(1);

            // Use this function to enable/disable Torch
            //qrCodeReaderView.setTorchEnabled(true);

            // Use this function to set front camera preview
            //qrCodeReaderView.setFrontCamera();

            // Use this function to set back camera preview
            qrCodeReaderView.setBackCamera();

        } catch (Exception e) {

        }
    }

    @Override
    public void onQRCodeRead(final String text, PointF[] points) {
        qrCodeReaderView.setQRDecodingEnabled(false);
        boolean retryFlag = true;

        switch(qrMode) {
            case ADDRESS_REGISTRATION:
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    AddressRegistrationReq adrRegReq = objectMapper.readValue(text, AddressRegistrationReq.class);
                    if (!adrRegReq.url.startsWith("https://") ||
                        !adrRegReq.action.equals("register")) {
                        throw new Exception("malformed json.");
                    }
                    Log.d(this.getClass().getName(), "Captured QR: " + objectMapper.writeValueAsString(adrRegReq));
                    retryFlag = false;
                } catch (Exception e) {
                }
                break;
            case TRANSACTION_SIGNING:
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    TransactionSigningReq transactionSigningReq = objectMapper.readValue(text, TransactionSigningReq.class);
                    if (!transactionSigningReq.url.startsWith("https://") ||
                            !transactionSigningReq.action.equals("sign")) {
                        throw new Exception("malformed json.");
                    }
                    Log.d(this.getClass().getName(), "Captured QR: " + objectMapper.writeValueAsString(transactionSigningReq));
                    retryFlag = false;
                } catch (Exception e) {
                }
                break;
            default:
                break;
        }

        if (retryFlag) {
            qrCodeReaderView.setQRDecodingEnabled(true);
            return;
        }

        //Toast.makeText(context, "Loading please wait...", Toast.LENGTH_SHORT).show();

        Message msg = new Message();
        msg.obj = "Received :\n" + text;
        uiThread.sendMessage(msg);

        Thread t = new Thread() {
            @Override
            public void run() {
                qrCodeReaderView.stopCamera();
                if (callback != null) {
                    callback.callbackCall(text);
                }
            }
        };
        t.start();
    }

    private Handler uiThread = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            textView.setText(msg.obj.toString());
        }
    };

}

