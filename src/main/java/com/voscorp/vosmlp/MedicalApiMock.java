package com.voscorp.vosmlp;

import org.springframework.stereotype.Service;

@Service
public class MedicalApiMock {

    // Имитация: реальных медицинских баз нет, результат зависит от заявления
    public MedicalResult verify(AbsenceApplication app) {
        if (app.getDocumentType().equals("Другой документ")) {
            return MedicalResult.NOT_FOUND;
        }
        if (app.getReason().toLowerCase().contains("ошибка")) {
            return MedicalResult.DATA_MISMATCH;
        }
        return MedicalResult.VALID;
    }
}