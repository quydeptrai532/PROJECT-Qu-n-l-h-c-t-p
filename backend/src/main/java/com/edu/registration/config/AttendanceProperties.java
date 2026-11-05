package com.edu.registration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cấu hình tập trung cho quy tắc tính chuyên cần (QLSV-70).
 * Các tham số này có thể cấu hình động qua application.yml hoặc biến môi trường.
 * Các giá trị mặc định được thiết lập an toàn trong khi chờ Chủ dự án chốt chính thức quy chế.
 */
@Component
@ConfigurationProperties(prefix = "app.attendance")
public class AttendanceProperties {

    /**
     * Danh sách trạng thái được tính là vắng (mặc định: VANG).
     */
    private String trangThaiTinhVang = "VANG";

    /**
     * Vắng có phép (CO_PHEP) có bị tính vào số buổi vắng không (mặc định: false).
     */
    private boolean coPhepTinhVang = false;

    /**
     * Số lần đi muộn quy đổi thành 1 lần vắng (0 = không quy đổi; mặc định 0).
     */
    private int soLanMuonThanhMotVang = 0;

    /**
     * Mẫu số tính tỷ lệ chuyên cần: SO_BUOI_DA_DIEM_DANH (mặc định) hoặc SO_BUOI_QUY_DINH.
     */
    private String mauSoChuyenCan = "SO_BUOI_DA_DIEM_DANH";

    /**
     * Ghi chú chính sách hiển thị cho người dùng và giảng viên.
     */
    private String ghiChuQuyChe = "Chính sách quy đổi muộn sang vắng, xử lý vắng có phép và mẫu số tỷ lệ chuyên cần đang áp dụng cấu hình dự kiến và chờ Chủ dự án phê duyệt chính thức.";

    public String getTrangThaiTinhVang() {
        return trangThaiTinhVang;
    }

    public void setTrangThaiTinhVang(String trangThaiTinhVang) {
        this.trangThaiTinhVang = trangThaiTinhVang;
    }

    public boolean isCoPhepTinhVang() {
        return coPhepTinhVang;
    }

    public void setCoPhepTinhVang(boolean coPhepTinhVang) {
        this.coPhepTinhVang = coPhepTinhVang;
    }

    public int getSoLanMuonThanhMotVang() {
        return soLanMuonThanhMotVang;
    }

    public void setSoLanMuonThanhMotVang(int soLanMuonThanhMotVang) {
        this.soLanMuonThanhMotVang = soLanMuonThanhMotVang;
    }

    public String getMauSoChuyenCan() {
        return mauSoChuyenCan;
    }

    public void setMauSoChuyenCan(String mauSoChuyenCan) {
        this.mauSoChuyenCan = mauSoChuyenCan;
    }

    public String getGhiChuQuyChe() {
        return ghiChuQuyChe;
    }

    public void setGhiChuQuyChe(String ghiChuQuyChe) {
        this.ghiChuQuyChe = ghiChuQuyChe;
    }
}
