package com.nfcweblink.infineon.exceptions;

public class GetKeyInfoException extends NfcCardException {

    public GetKeyInfoException(int SW1SW2, String message) {
        super(SW1SW2, message);
    }

}
