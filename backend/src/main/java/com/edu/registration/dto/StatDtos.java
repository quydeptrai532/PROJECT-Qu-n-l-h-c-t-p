package com.edu.registration.dto;

public final class StatDtos {

    private StatDtos() {
    }

    public record TongQuanDto(long soSinhVien, long soGiangVien, long soMonHoc, long soLopHocPhan,
                              long soDangKy, String hocKyHienTai) {
    }

    public record ThongKeMonDto(Long monHocId, String maMon, String tenMon, int soLop,
                                int tongChiTieu, int daDangKy, double tiLeLapDay) {
    }
}
