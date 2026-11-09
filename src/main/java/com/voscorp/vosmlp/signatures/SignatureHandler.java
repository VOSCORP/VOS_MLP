package com.voscorp.vosmlp.signatures;

import com.voscorp.vosmlp.auth.AppUser;

public interface SignatureHandler {
    SignAction action();

    void onConfirmed(SignatureRequest request, AppUser signer);
}