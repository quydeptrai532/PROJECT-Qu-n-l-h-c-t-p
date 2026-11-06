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

    public record CanhBaoHocVuDto(String loai, String tieuDe, String lyDo, String mucTinh,
                                  Long lopHocPhanId, String maLop, Long monHocId, String maMon,
                                  String tenMon, int soLop, long daDangKy, long tongChiTieu,
                                  double tiLeLapDay, Long hocKyId, String tenHocKy,
                                  java.time.LocalDate dangKyDen, String duongDan) {
    }

    public record HanhDongHocVuDto(Long hocKyId, String tenHocKy, Long khoaId, String tenKhoa,
                                   java.time.LocalDateTime capNhatLuc, java.util.List<CanhBaoHocVuDto> canhBao) {
    }
}
