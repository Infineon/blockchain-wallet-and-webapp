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

import android.util.Log;

import com.nfcweblink.models.TransactionSignature;

import org.web3j.crypto.ECDSASignature;
import org.web3j.crypto.Keys;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.Sign;
import org.web3j.crypto.TransactionDecoder;
import org.web3j.utils.Bytes;
import org.web3j.utils.Convert;

import java.math.BigInteger;
import java.util.Arrays;

public class EthereumService {

    private String serialized;
    private RawTransaction rawTransaction;

    public EthereumService(String serialized) {
        this.serialized = serialized;
        try {
            rawTransaction = TransactionDecoder.decode(serialized);
        } catch (Exception e) {
            rawTransaction = null;
        }
    }

    public RawTransaction getRawTransaction() {
        return rawTransaction;
    }

    public String getTo() {
        if (rawTransaction != null) {
            return Keys.toChecksumAddress(rawTransaction.getTo());
        }
        return null;
    }

    public String getGasPriceWei() {
        if (rawTransaction != null) {
            return rawTransaction.getGasPrice().toString();
        }
        return null;
    }

    public String getGasPriceEther() {
        String wei = getGasPriceWei();
        if (wei != null) {
            return Convert.fromWei(wei, Convert.Unit.ETHER).toPlainString();
        }
        return null;
    }

    public String getGasLimit() {
        if (rawTransaction != null) {
            return rawTransaction.getGasLimit().toString();
        }
        return null;
    }

    public String getNonce() {
        if (rawTransaction != null) {
            return rawTransaction.getNonce().toString();
        }
        return null;
    }

    public String getValueWei() {
        if (rawTransaction != null) {
            return rawTransaction.getValue().toString();
        }
        return null;
    }

    public String getValueEther() {
        String wei = getValueWei();
        if (wei != null) {
            return Convert.fromWei(wei, Convert.Unit.ETHER).toPlainString();
        }
        return null;
    }

    public String computeFee(String gas) {
        String gasPrice = getGasPriceWei();
        if (gasPrice != null && gas != null) {
            String wei = new BigInteger(gasPrice).multiply(new BigInteger(gas)).toString(10);
            return Convert.fromWei(wei, Convert.Unit.ETHER).toPlainString();
        }
        return null;
    }

    public static TransactionSignature convertToEthereumSignature(byte[] pubKey, byte[] hash, byte[] rawSignature) {
        try {
            byte[] r = Bytes.trimLeadingZeroes(extractR(rawSignature));
            byte[] s = Bytes.trimLeadingZeroes(extractS(rawSignature));
            s = getCanonicalisedS(r, s);
            byte[] v = new byte[1];
            v[0] = getV(pubKey, hash, r, s);

            TransactionSignature ts = new TransactionSignature(v, r, s);
            return ts;
        } catch (Exception e) {
            Log.e(EthereumService.class.getName(), "convertToEthereumSignature exception: " + e.getMessage());
            return null;
        }
    }

    public static byte[] getCanonicalisedS(byte[] r, byte[] s) {
        ECDSASignature ecdsaSignature = new ECDSASignature(new BigInteger(1, r), new BigInteger(1, s));
        ecdsaSignature = ecdsaSignature.toCanonicalised();
        return ecdsaSignature.s.toByteArray();
    }

    public static byte[] extractR(byte[] signature) {
        int startR = (signature[1] & 0x80) != 0 ? 3 : 2;
        int lengthR = signature[startR + 1];
        return Arrays.copyOfRange(signature, startR + 2, startR + 2 + lengthR);
    }

    public static byte[] extractS(byte[] signature) {
        int startR = (signature[1] & 0x80) != 0 ? 3 : 2;
        int lengthR = signature[startR + 1];
        int startS = startR + 2 + lengthR;
        int lengthS = signature[startS + 1];
        return Arrays.copyOfRange(signature, startS + 2, startS + 2 + lengthS);
    }

    public static byte getV(byte[] publicKey, byte[] hashedTransaction, byte[] r, byte[] s) {
        ECDSASignature sig = new ECDSASignature(new BigInteger(1, r), new BigInteger(1, s));
        // Now we have to work backwards to figure out the recId needed to recover the signature.
        int recId = -1;
        for (int i = 0; i < 4; i++) {

            //calls private method form web3j lib
            BigInteger k = Sign.recoverFromSignature(i, sig, hashedTransaction);
            if (k != null && k.equals(new BigInteger(1, publicKey))) {
                recId = i;
                break;
            }
        }
        if (recId == -1) {
            throw new RuntimeException(
                    "Could not construct a recoverable key. This should never happen.");
        }

        int headerByte = recId + 27;
        // 1 header + 32 bytes for R + 32 bytes for S
        return (byte) headerByte;
    }
}
