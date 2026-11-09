package com.voscorp.vosmlp.applications;

import com.voscorp.vosmlp.documents.MedicalResult;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class MedicalApiMock {

    // MED- значит VALID, BAD- значит DATA_MISMATCH, остальные NOT_FOUND
    public MedicalResult verify(String certificateNumber) {
        String n = certificateNumber == null ? "" : certificateNumber.trim().toUpperCase(Locale.ROOT);
        if (n.startsWith("MED-")) { return MedicalResult.VALID; }
        if (n.startsWith("BAD-")) { return MedicalResult.DATA_MISMATCH; }
        return MedicalResult.NOT_FOUND;
    }
}