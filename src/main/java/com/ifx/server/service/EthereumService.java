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
package com.ifx.server.service;

import com.ifx.server.model.Transact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Bytes;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * http://docs.web3j.io/4.8.4/
 * https://docs.web3j.io/latest/javadoc-api/
 * https://mvnrepository.com/artifact/org.web3j/core
 */
@Service
public class EthereumService {

    Logger logger = LoggerFactory.getLogger(EthereumService.class);

    public static final long CHAIN_ID_ROPSTEN = 3;
    public static final String NODE_URL_ROPSTEN = "https://api.myetherwallet.com/rop";

    Credentials credentials = null;

    private Web3j web3 = Web3j.build(new HttpService(NODE_URL_ROPSTEN));

    public EthereumService() {
        try {
            credentials = Credentials.create(Keys.createEcKeyPair());
        } catch (Exception e) {
            logger.error("EthereumService() exception: " + e.getMessage());
        }
    }

    public String getClientVersion() {
        try {
            Web3ClientVersion web3ClientVersion = web3.web3ClientVersion().sendAsync().get();
            String clientVersion = web3ClientVersion.getWeb3ClientVersion();
            return clientVersion;
        } catch (Exception e) {
            logger.error("getClientVersion() exception: " + e.getMessage());
        }
        return "";
    }

    public String getBlockHeight() {
        try {
            EthBlockNumber ebn = web3.ethBlockNumber().sendAsync().get();
            String height = "0x" + ebn.getBlockNumber().toString(16);
            return height;
        } catch (Exception e) {
            logger.error("getBlockHeight() exception: " + e.getMessage());
        }
        return "";
    }

    public String getBalance(String address) {
        try {
            EthGetBalance egb = web3.ethGetBalance(address, DefaultBlockParameterName.LATEST).sendAsync().get();
            String balance = "0x" + egb.getBalance().toString(16);
            return balance;
        } catch (Exception e) {
            logger.error("getBalance() exception: " + e.getMessage());
        }
        return "";
    }

    public String getTransactionCount(String address) {
        try {
            EthGetTransactionCount egtc = web3.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST).sendAsync().get();
            String balance = "0x" + egtc.getTransactionCount().toString(16);
            return balance;
        } catch (Exception e) {
            logger.error("getTransactionCount() exception: " + e.getMessage());
        }
        return "";
    }

    public String getGasPrice() {
        try {
            EthGasPrice egp = web3.ethGasPrice().sendAsync().get();
            String gasPrice = "0x" + egp.getGasPrice().toString(16);
            return gasPrice;
        } catch (Exception e) {
            logger.error("getGasPrice() exception: " + e.getMessage());
        }
        return "";
    }

    public String estimateEtherTransactionGas() {
        try {
            String from = "0x687422eEA2cB73B5d3e242bA5456b782919AFc85";
            String to = "0xd7a0a13c3206DACD44f3218c07ee809826FACCE5";
            BigInteger value = Convert.toWei("1", Convert.Unit.ETHER).toBigInteger();
            BigInteger nonce = new BigInteger(getTransactionCount(from).substring(2), 16);

            Transaction transaction = Transaction.createEtherTransaction(from, nonce, DefaultGasProvider.GAS_PRICE,
                    DefaultGasProvider.GAS_LIMIT, to, value);

            EthEstimateGas eeg = web3.ethEstimateGas(transaction).sendAsync().get();
            String estimatedGas = "0x" + eeg.getAmountUsed().toString(16);

            return estimatedGas;
        } catch (Exception e) {
            logger.error("estimateEtherTransactionGas() exception: " + e.getMessage());
        }
        return "";
    }

    public boolean verifyTransactionHash(Transact transact) {
        try {
            BigInteger nonce = new BigInteger(transact.getNonce().substring(2), 16);
            BigInteger gasPrice = new BigInteger(transact.getGasPrice().substring(2), 16);
            BigInteger gasLimit = new BigInteger(transact.getGasLimit().substring(2), 16);
            BigInteger value = new BigInteger(transact.getValue().substring(2), 16);

            /* Generate from scratch */
            RawTransaction rawTransaction1 = RawTransaction.createEtherTransaction(nonce,
                    gasPrice, gasLimit, transact.getTo(), value);
            byte[] rawTransactionEncoded1 = TransactionEncoder.encode(rawTransaction1, CHAIN_ID_ROPSTEN);
            String hash1 = Numeric.toHexString(Hash.sha3(rawTransactionEncoded1));

            /* Decode serialized */
            RawTransaction rawTransaction2 = TransactionDecoder.decode(transact.getSerialized());
            byte[] rawTransactionEncoded2 = TransactionEncoder.encode(rawTransaction2, CHAIN_ID_ROPSTEN);
            String hash2 = Numeric.toHexString(Hash.sha3(rawTransactionEncoded2));


            if (hash1.equals(transact.getHash()) && hash2.equals(transact.getHash()))
                return true;

            return false;
        } catch (Exception e) {
            logger.error("verifyTransactionHash() exception: " + e.getMessage());
        }
        return false;
    }

    public boolean verifySendTransactionWithSoftWallet(Transact transact) {
        try {
            /* Generate from scratch *//*
            BigInteger nonce = new BigInteger(transact.getNonce().substring(2), 16);
            BigInteger gasPrice = new BigInteger(transact.getGasPrice().substring(2), 16);
            BigInteger gasLimit = new BigInteger(transact.getGasLimit().substring(2), 16);
            BigInteger value = new BigInteger(transact.getValue().substring(2), 16);

            RawTransaction rawTransaction = RawTransaction.createEtherTransaction(nonce,
                    BigInteger.valueOf(0), gasLimit, transact.getTo(), value);
            byte[] rawTransactionEncoded = TransactionEncoder.encode(rawTransaction, CHAIN_ID_ROPSTEN);*/

            /* Decode serialized */
            RawTransaction rawTransaction = TransactionDecoder.decode(transact.getSerialized());
            byte[] rawTransactionEncoded = TransactionEncoder.encode(rawTransaction, CHAIN_ID_ROPSTEN);

            /* Sign with software wallet */
            byte[] hash = Hash.sha3(rawTransactionEncoded);
            Sign.SignatureData signatureData = Sign.signMessage(hash, credentials.getEcKeyPair(), false);
            byte[] publicKey = Numeric.hexStringToByteArray(credentials.getEcKeyPair().getPublicKey().toString(16));

            /* Decode signature */
            byte[] r = signatureData.getR();
            byte[] s = signatureData.getS();
            s = getCanonicalisedS(r, s);
            byte v = getV(publicKey, hash, r, s);

            signatureData = new Sign.SignatureData(v, r, s);
            signatureData = TransactionEncoder.createEip155SignatureData(signatureData, CHAIN_ID_ROPSTEN);

            String hexValue = Numeric.toHexString(encode(rawTransaction, signatureData));
            EthSendTransaction ethSendTransaction = web3.ethSendRawTransaction(hexValue).sendAsync().get();

            if (ethSendTransaction != null && ethSendTransaction.getError() != null) {
                if (ethSendTransaction.getError().getMessage().contains("insufficient funds")) {
                    return true;
                }
                logger.info("sendTransactionWithSoftWallet() exception: " + ethSendTransaction.getError().getMessage());
            }

            return false;
        } catch (Exception e) {
            logger.error("sendTransactionWithSoftWallet() exception: " + e.getMessage());
        }
        return false;
    }


    public String sendTransaction(String serialized, String publicKey, String signature) {
        try {
            /* Decode serialized */
            RawTransaction rawTransaction = TransactionDecoder.decode(serialized);
            byte[] rawTransactionEncoded = TransactionEncoder.encode(rawTransaction, CHAIN_ID_ROPSTEN);
            byte[] hash = Hash.sha3(rawTransactionEncoded);

            /* Decode signature */
            byte[] sig = Numeric.hexStringToByteArray(signature);
            byte[] r = Bytes.trimLeadingZeroes(extractR(sig));
            byte[] s = Bytes.trimLeadingZeroes(extractS(sig));
            s = getCanonicalisedS(r, s);
            byte v = getV(Numeric.hexStringToByteArray(publicKey), hash, r, s);

            Sign.SignatureData signatureData = new Sign.SignatureData(v, r, s);
            signatureData = TransactionEncoder.createEip155SignatureData(signatureData, CHAIN_ID_ROPSTEN);

            String hexValue = Numeric.toHexString(encode(rawTransaction, signatureData));
            EthSendTransaction ethSendTransaction = web3.ethSendRawTransaction(hexValue).sendAsync().get();

            if (ethSendTransaction != null && ethSendTransaction.getError() != null) {
                logger.info("sendTransaction() exception: " + ethSendTransaction.getError().getMessage());
                return null;
            }

            return ethSendTransaction.getTransactionHash();
        } catch (Exception e) {
            logger.error("sendTransaction() exception: " + e.getMessage());
        }
        return null;
    }

    public boolean isValidAddress(String address) {
        try {
            String trim = address;
            if (address.startsWith("0x") || address.startsWith("0X"))
                trim = address.substring(2);
            return WalletUtils.isValidAddress(trim);
        } catch (Exception e) {
            logger.error("verifyAddress() exception: " + e.getMessage());
        }
        return false;
    }

    public String sha3(String data) {
        try {
            return Hash.sha3(data);
        } catch (Exception e) {
            logger.error("sha3() exception: " + e.getMessage());
        }
        return null;
    }

    public String toAddress(String publicKey) {
        try {
            return Keys.toChecksumAddress(Keys.getAddress(publicKey));
        } catch (Exception e) {
            logger.error("toAddress() exception: " + e.getMessage());
        }
        return null;
    }

    public String toChecksumAddress(String address) {
        try {
            return Keys.toChecksumAddress(address);
        } catch (Exception e) {
            logger.error("toChecksumAddress() exception: " + e.getMessage());
        }
        return null;
    }

    /*********************/


    private static byte[] getCanonicalisedS(byte[] r, byte[] s) {
        ECDSASignature ecdsaSignature = new ECDSASignature(new BigInteger(1, r), new BigInteger(1, s));
        ecdsaSignature = ecdsaSignature.toCanonicalised();
        return ecdsaSignature.s.toByteArray();
    }

    private static byte[] extractR(byte[] signature) {
        int startR = (signature[1] & 0x80) != 0 ? 3 : 2;
        int lengthR = signature[startR + 1];
        return Arrays.copyOfRange(signature, startR + 2, startR + 2 + lengthR);
    }

    private static byte[] extractS(byte[] signature) {
        int startR = (signature[1] & 0x80) != 0 ? 3 : 2;
        int lengthR = signature[startR + 1];
        int startS = startR + 2 + lengthR;
        int lengthS = signature[startS + 1];
        return Arrays.copyOfRange(signature, startS + 2, startS + 2 + lengthS);
    }

    private static byte getV(byte[] publicKey, byte[] hashedTransaction, byte[] r, byte[] s) {
        ECDSASignature sig = new ECDSASignature(new BigInteger(1, r), new BigInteger(1, s));
        // Now we have to work backwards to figure out the recId needed to recover the signature.
        int recId = -1;
        for (int i = 0; i < 4; i++) {

            //calls private method form web3j lib
            BigInteger k = Sign.recoverFromSignature(i, sig, hashedTransaction);
            byte[] test = Numeric.hexStringToByteArray( k.toString());
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

    private static byte[] encode(RawTransaction rawTransaction, Sign.SignatureData signatureData) {
        List<RlpType> values = asRlpValues(rawTransaction, signatureData);
        RlpList rlpList = new RlpList(values);
        return RlpEncoder.encode(rlpList);
    }

    private static List<RlpType> asRlpValues(
            RawTransaction rawTransaction, Sign.SignatureData signatureData) {
        List<RlpType> result = new ArrayList<RlpType>();

        result.add(RlpString.create(rawTransaction.getNonce()));
        result.add(RlpString.create(rawTransaction.getGasPrice()));
        result.add(RlpString.create(rawTransaction.getGasLimit()));

        // an empty to address (contract creation) should not be encoded as a numeric 0 value
        String to = rawTransaction.getTo();
        if (to != null && to.length() > 0) {
            // addresses that start with zeros should be encoded with the zeros included, not
            // as numeric values
            result.add(RlpString.create(Numeric.hexStringToByteArray(to)));
        } else {
            result.add(RlpString.create(""));
        }

        result.add(RlpString.create(rawTransaction.getValue()));

        // value field will already be hex encoded, so we need to convert into binary first
        byte[] data = Numeric.hexStringToByteArray(rawTransaction.getData());
        result.add(RlpString.create(data));

        if (signatureData != null) {
            result.add(RlpString.create(signatureData.getV()));
            result.add(RlpString.create(Bytes.trimLeadingZeroes(signatureData.getR())));
            result.add(RlpString.create(Bytes.trimLeadingZeroes(signatureData.getS())));
        }

        return result;
    }
}
