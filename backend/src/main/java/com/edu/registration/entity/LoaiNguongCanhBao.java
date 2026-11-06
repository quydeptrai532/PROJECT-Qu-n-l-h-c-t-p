package com.edu.registration.entity;

public enum LoaiNguongCanhBao {
    SO_BUOI_VANG("Số buổi vắng"),
    TY_LE_VANG("Tỷ lệ vắng");

    private final String moTa;

    LoaiNguongCanhBao(String moTa) {
        this.moTa = moTa;
    }

    public String getMoTa() {
        return moTa;
    }
}
