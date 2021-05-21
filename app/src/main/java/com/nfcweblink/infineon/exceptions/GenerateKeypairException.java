package com.nfcweblink.infineon.exceptions;

public class GenerateKeypairException extends NfcCardException {

    public GenerateKeypairException(int SW1SW2, String message) {
        super(SW1SW2, message);
    }

}
