package com.edu.registration.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "lop_hoc_phan", indexes = {
        @Index(name = "idx_lop_hoc_ky", columnList = "hoc_ky_id"),
        @Index(name = "idx_lop_giang_vien", columnList = "giang_vien_id"),
        @Index(name = "idx_lop_mon_hoc", columnList = "mon_hoc_id")
})
public class LopHocPhan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String ma;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mon_hoc_id", nullable = false)
    private MonHoc monHoc;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hoc_ky_id", nullable = false)
    private HocKy hocKy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "giang_vien_id", nullable = false)
    private GiangVien giangVien;

    @Column(nullable = false, length = 20)
    private String phong;

    @Column(nullable = false)
    private int thu;

    @Column(nullable = false)
    private int tietBatDau;

    @Column(nullable = false)
    private int soTiet;

    @Column(nullable = false)
    private int siSoToiDa;

    @Column(nullable = false)
    private int siSoHienTai = 0;

    public int getTietKetThuc() {
        return tietBatDau + soTiet - 1;
    }

    public int getConLai() {
        return Math.max(0, siSoToiDa - siSoHienTai);
    }

    public boolean trungLichVoi(LopHocPhan khac) {
        if (khac == null || thu != khac.getThu()) {
            return false;
        }
        return tietBatDau <= khac.getTietKetThuc() && khac.getTietBatDau() <= getTietKetThuc();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMa() {
        return ma;
    }

    public void setMa(String ma) {
        this.ma = ma;
    }

    public MonHoc getMonHoc() {
        return monHoc;
    }

    public void setMonHoc(MonHoc monHoc) {
        this.monHoc = monHoc;
    }

    public HocKy getHocKy() {
        return hocKy;
    }

    public void setHocKy(HocKy hocKy) {
        this.hocKy = hocKy;
    }

    public GiangVien getGiangVien() {
        return giangVien;
    }

    public void setGiangVien(GiangVien giangVien) {
        this.giangVien = giangVien;
    }

    public String getPhong() {
        return phong;
    }

    public void setPhong(String phong) {
        this.phong = phong;
    }

    public int getThu() {
        return thu;
    }

    public void setThu(int thu) {
        this.thu = thu;
    }

    public int getTietBatDau() {
        return tietBatDau;
    }

    public void setTietBatDau(int tietBatDau) {
        this.tietBatDau = tietBatDau;
    }

    public int getSoTiet() {
        return soTiet;
    }

    public void setSoTiet(int soTiet) {
        this.soTiet = soTiet;
    }

    public int getSiSoToiDa() {
        return siSoToiDa;
    }

    public void setSiSoToiDa(int siSoToiDa) {
        this.siSoToiDa = siSoToiDa;
    }

    public int getSiSoHienTai() {
        return siSoHienTai;
    }

    public void setSiSoHienTai(int siSoHienTai) {
        this.siSoHienTai = siSoHienTai;
    }
}
