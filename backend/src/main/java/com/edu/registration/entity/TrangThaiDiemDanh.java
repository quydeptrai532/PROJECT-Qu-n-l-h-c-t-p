package com.edu.registration.entity;

public enum TrangThaiDiemDanh {
    CO_MAT("Có mặt"),
    VANG("Vắng"),
    MUON("Muộn"),
    CO_PHEP("Có phép");

    private final String moTa;

    TrangThaiDiemDanh(String moTa) {
        this.moTa = moTa;
    }

    public String getMoTa() {
        return moTa;
    }
}
