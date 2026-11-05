package com.edu.registration.entity;

public enum TrangThaiBangDiem {
    NHAP("Nháp"),
    CHO_DUYET("Chờ duyệt"),
    DA_DUYET("Đã duyệt"),
    DA_CONG_BO("Đã công bố");

    private final String moTa;

    TrangThaiBangDiem(String moTa) {
        this.moTa = moTa;
    }

    public String getMoTa() {
        return moTa;
    }
}
