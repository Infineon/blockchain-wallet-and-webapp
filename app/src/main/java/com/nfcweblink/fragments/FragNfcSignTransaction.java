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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.nfcweblink.infineon.apdu.response.GenerateSignatureResponseApdu;
import com.nfcweblink.infineon.apdu.response.GetKeyInfoResponseApdu;
import com.nfcweblink.models.TransactionSignature;
import com.nfcweblink.models.TransactionSigningReq;
import com.nfcweblink.models.TransactionSigningResp;
import com.nfcweblink.utils.EthereumService;
import com.nfcweblink.utils.FragmentService;
import com.nfcweblink.utils.FragmentServiceInterface;
import com.nfcweblink.utils.HttpsTrustManager;
import com.nfcweblink.utils.IsoTagWrapper;
import com.nfcweblink.utils.MailBox;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.security.Provider;
import java.security.Security;

import static com.nfcweblink.utils.Constants.CHAIN_ID;
import static com.nfcweblink.utils.Constants.HARDWARE_WALLET_KEY_HANDLE;
import static org.web3j.crypto.TransactionEncoder.encode;

public class FragNfcSignTransaction extends Fragment implements FragmentServiceInterface {

    private String title = "AUTHORIZE TRANSACTION";
    private MailBox mailBox;
    private TextView textViewNFC;
    private TransactionSigningReq transactionSigningReq;
    private EthereumService ethereumService;

    public FragNfcSignTransaction() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_nfc_sign_transaction, container, false);

        // Setup global mailbox
        mailBox = (MailBox)getContext().getApplicationContext();

        // Change title
        getActivity().setTitle(title);

        // Get textView
        textViewNFC = view.findViewById(R.id.textViewNFC);

        return view;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Display information about the transaction before signing
        try {
            String data = mailBox.receiveMail(MailBox.Topic.TRANSACTION_SIGNING);
            ObjectMapper objectMapper = new ObjectMapper();
            transactionSigningReq = objectMapper.readValue(data, TransactionSigningReq.class);
            ethereumService = new EthereumService(transactionSigningReq.serialized);

            TextView textViewFrom = view.findViewById(R.id.textViewFrom);
            textViewFrom.setText(transactionSigningReq.signer);

            TextView textViewTo = view.findViewById(R.id.textViewTo);
            textViewTo.setText(ethereumService.getTo());

            TextView textViewValue = view.findViewById(R.id.textViewValue);
            textViewValue.setText(ethereumService.getValueEther());

            TextView textViewGasLimit = view.findViewById(R.id.textViewGasLimit);
            textViewGasLimit.setText(ethereumService.getGasLimit());

            TextView textViewGasPrice = view.findViewById(R.id.textViewGasPrice);
            textViewGasPrice.setText(ethereumService.getGasPriceEther());

            TextView textViewEstimatedGas = view.findViewById(R.id.textViewEstimatedGas);
            textViewEstimatedGas.setText(transactionSigningReq.gasEstimation);

            TextView textViewEstimatedFee = view.findViewById(R.id.textViewEstimatedFee);
            textViewEstimatedFee.setText(ethereumService.computeFee(transactionSigningReq.gasEstimation));

            TextView textViewNonce = view.findViewById(R.id.textViewNonce);
            textViewNonce.setText(ethereumService.getNonce());

            //verifyWithSoftWallet();

        } catch (Exception e) {
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(this.getClass().getName(), "Exception while decoding serialized transaction: " + e.getMessage());
            FragmentService.reset(getActivity());
        }
    }

    @Override
    public void onNfcCallback(IsoDep isoDep) {
        try {
            RawTransaction rawTransaction = ethereumService.getRawTransaction();
            byte[] encodedTransaction = encode(rawTransaction, CHAIN_ID);
            final byte[] hashedTransaction = Hash.sha3(encodedTransaction);

            GetKeyInfoResponseApdu getKeyInfoResponseApdu = NfcUtils.readPublicKeyOrCreateIfNotExists(IsoTagWrapper.of(isoDep), HARDWARE_WALLET_KEY_HANDLE);
            String pubKeyStringWithOutPrefix = getKeyInfoResponseApdu.getPublicKeyInHexWithoutPrefix().toLowerCase(); // Remove starting '0x04' byte
            String address = Keys.toChecksumAddress(Keys.getAddress(pubKeyStringWithOutPrefix));

            if (!address.equals(transactionSigningReq.signer))
                new Exception("Transaction sender address mismatch with wallet address!");

            final GenerateSignatureResponseApdu signedTransaction = NfcUtils.generateSignature(IsoTagWrapper.of(isoDep),
                    HARDWARE_WALLET_KEY_HANDLE, hashedTransaction, null);

            String response = Numeric.toHexString(signedTransaction.getSignature());

            /*TransactionSignature transactionSignature = EthereumService.convertToEthereumSignature(
                    Numeric.hexStringToByteArray(pubKeyStringWithOutPrefix), hashedTransaction, signedTransaction.getSignature());

            String response = "\n{v: " + Numeric.toHexString(transactionSignature.v) + ",\nr: "
                    + Numeric.toHexString(transactionSignature.r) + ",\ns: " + Numeric.toHexString(transactionSignature.s) + "}";*/

            textViewNFC.setText("Received signature: " + response + "\n\nConnecting to server now...");
            Log.d(this.getClass().getName(), "Received signature: " + response);
            dispatch("0x" + pubKeyStringWithOutPrefix, response);
        } catch (Exception e) {
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(this.getClass().getName(), "Exception while reading public key from card: " + e.getMessage());
        }
    }

    /**
     * Connect to server
     * Warning: Connection is configured to accept all server SSL cert
     *          this should not be used in production
     * @param pubkey
     * @param signature
     */
    private void dispatch(final String pubkey, final String signature) {
        RequestQueue queue = Volley.newRequestQueue(getContext());
        String url = transactionSigningReq.url + "?token=" + transactionSigningReq.token
                 + "&pubkey=" + pubkey
                 + "&signature=" + signature;

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
                        TransactionSigningResp tsr = objectMapper.readValue(jsonError, TransactionSigningResp.class);
                        if (tsr.message.contains("No pending transaction")) {
                            Toast.makeText(getContext(), "Wrong wallet", Toast.LENGTH_SHORT).show();
                            Log.d(this.getClass().getName(), "Wrong wallet");
                        } else {
                            String trim = tsr.message.substring(tsr.message.lastIndexOf(":") + 1);
                            Toast.makeText(getContext(), trim, Toast.LENGTH_SHORT).show();
                            Log.d(this.getClass().getName(), trim);
                        }
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

    private void verifyWithSoftWallet() {
        try {
            setupBouncyCastle();
            Credentials credentials = Credentials.create(Keys.createEcKeyPair());

            RawTransaction rawTransaction = ethereumService.getRawTransaction();
            byte[] encodedTransaction = encode(rawTransaction, CHAIN_ID);
            byte[] hashedTransaction = Hash.sha3(encodedTransaction);

            Sign.SignatureData signatureData = Sign.signMessage(hashedTransaction, credentials.getEcKeyPair(), false);
            byte[] pubKey = Numeric.hexStringToByteArray(credentials.getEcKeyPair().getPublicKey().toString(16));

            /* Decode signature */
            byte[] r = signatureData.getR();
            byte[] s = signatureData.getS();
            byte[] v = new byte[1];
            s = EthereumService.getCanonicalisedS(r, s);
            v[0] = EthereumService.getV(pubKey, hashedTransaction, r, s);

            TransactionSignature ts = new TransactionSignature(v, r, s);
        } catch (Exception e) {
            Log.e(this.getClass().getName(), "verifyWithSoftWallet exception: " + e.getMessage());
        }
    }

    /**
     * https://github.com/web3j/web3j/issues/915
     */
    private void setupBouncyCastle() {
        final Provider provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
        if (provider == null) {
            // Web3j will set up the provider lazily when it's first used.
            return;
        }
        if (provider.getClass().equals(BouncyCastleProvider.class)) {
            // BC with same package name, shouldn't happen in real life.
            return;
        }
        // Android registers its own BC provider. As it might be outdated and might not include
        // all needed ciphers, we substitute it with a known BC bundled in the app.
        // Android's BC has its package rewritten to "com.android.org.bouncycastle" and because
        // of that it's possible to have another BC implementation loaded in VM.
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
    }
}
