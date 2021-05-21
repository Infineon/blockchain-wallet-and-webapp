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
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nfcweblink.R;
import com.nfcweblink.infineon.NfcUtils;
import com.nfcweblink.infineon.apdu.response.GetKeyInfoResponseApdu;
import com.nfcweblink.models.AddressRegistrationReq;
import com.nfcweblink.models.AddressRegistrationResp;
import com.nfcweblink.utils.FragmentService;
import com.nfcweblink.utils.FragmentServiceInterface;
import com.nfcweblink.utils.HttpsTrustManager;
import com.nfcweblink.utils.IsoTagWrapper;
import com.nfcweblink.utils.MailBox;

import org.web3j.crypto.Keys;

import static com.nfcweblink.utils.Constants.HARDWARE_WALLET_KEY_HANDLE;

public class FragNfcReadAddress extends Fragment implements FragmentServiceInterface {

    private String title = "READ CARD ADDRESS";
    private MailBox mailBox;
    private TextView textView;

    public FragNfcReadAddress() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_nfc_read_address, container, false);

        // Setup global mailbox
        mailBox = (MailBox)getContext().getApplicationContext();

        // Change title
        getActivity().setTitle(title);

        // Get textView
        textView = view.findViewById(R.id.textView);

        return view;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public void onNfcCallback(IsoDep isoDep) {
        try {
            GetKeyInfoResponseApdu getKeyInfoResponseApdu = NfcUtils.readPublicKeyOrCreateIfNotExists(IsoTagWrapper.of(isoDep), HARDWARE_WALLET_KEY_HANDLE);
            String pubKey = getKeyInfoResponseApdu.getPublicKeyInHexWithoutPrefix();
            String address = Keys.getAddress(pubKey);
            address = Keys.toChecksumAddress(address);
            String data = mailBox.receiveMail(MailBox.Topic.ADDRESS_REGISTRATION);
            ObjectMapper objectMapper = new ObjectMapper();
            AddressRegistrationReq adrRegReq = objectMapper.readValue(data, AddressRegistrationReq.class);
            textView.setText("Received Address: " + address + "\n\nConnecting to server now...");
            Log.d(this.getClass().getName(), "Received Address: " + address + ". Connecting to server now...");
            dispatch(adrRegReq, pubKey);
        } catch (Exception e) {
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(this.getClass().getName(), "Exception while reading public key from card: " + e.getMessage());
        }
    }

    /**
     * Connect to server
     * Warning: Connection is configured to accept all server SSL cert
     *          this should not be used in production
     * @param adrReg
     * @param pubKey
     */
    private void dispatch(final AddressRegistrationReq adrReg, final String pubKey) {
        RequestQueue queue = Volley.newRequestQueue(getContext());
        String url = adrReg.url + "?token=" + adrReg.token + "&pubkey=" + pubKey;

        /**
         * Warning: this should not be used in production because it is vulnerable to SSL attacks
         */
        HttpsTrustManager.allowAllSSL();

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Toast.makeText(getContext(), "Sucessful!", Toast.LENGTH_SHORT).show();
                    Log.d(this.getClass().getName(), "Received server response OK");
                    FragmentService.reset(getActivity());
                }
            },
            new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse networkResponse = error.networkResponse;
                if (networkResponse != null && networkResponse.data != null) {
                    try {
                        String jsonError = new String(networkResponse.data);
                        ObjectMapper objectMapper = new ObjectMapper();
                        AddressRegistrationResp arr = objectMapper.readValue(jsonError, AddressRegistrationResp.class);
                        String trim = arr.message.substring(arr.message.lastIndexOf(":") + 1);
                        Toast.makeText(getContext(), trim, Toast.LENGTH_SHORT).show();
                        Log.d(this.getClass().getName(), trim);
                    } catch (Exception e) {
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.d(this.getClass().getName(), e.getMessage());
                    }
                } else {
                    Toast.makeText(getContext(), "Server connection error, try again!", Toast.LENGTH_SHORT).show();
                    Log.d(this.getClass().getName(), "Received server response KO");
                }
                FragmentService.reset(getActivity());
            }
        });

        queue.add(stringRequest);
    }
}
