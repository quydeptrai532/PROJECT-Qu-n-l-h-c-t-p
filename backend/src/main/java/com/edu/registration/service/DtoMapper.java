package com.edu.registration.service;

import com.edu.registration.dto.CatalogDtos;
import com.edu.registration.dto.LopHocPhanDtos;
import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.dto.UserDtos;
import com.edu.registration.entity.DangKyHocPhan;
import com.edu.registration.entity.Diem;
import com.edu.registration.entity.GiangVien;
import com.edu.registration.entity.HocKy;
import com.edu.registration.entity.Khoa;
import com.edu.registration.entity.LopHanhChinh;
import com.edu.registration.entity.LopHocPhan;
import com.edu.registration.entity.MonHoc;
import com.edu.registration.entity.MonTienQuyet;
import com.edu.registration.entity.Nganh;
import com.edu.registration.entity.SinhVien;
import org.springframework.stereotype.Component;

@Component
public class DtoMapper {

    public CatalogDtos.KhoaDto khoa(Khoa khoa) {
        return new CatalogDtos.KhoaDto(khoa.getId(), khoa.getMa(), khoa.getTen());
    }

    public CatalogDtos.NganhDto nganh(Nganh nganh) {
        return new CatalogDtos.NganhDto(nganh.getId(), nganh.getMa(), nganh.getTen(),
                nganh.getKhoa().getId(), nganh.getKhoa().getTen());
    }

    public CatalogDtos.LopHanhChinhDto lopHanhChinh(LopHanhChinh lop) {
        return new CatalogDtos.LopHanhChinhDto(lop.getId(), lop.getMa(), lop.getTen(),
                lop.getNganh().getId(), lop.getNganh().getTen(),
                lop.getNganh().getKhoa().getId(), lop.getNganh().getKhoa().getTen());
    }

    public CatalogDtos.MonHocDto monHoc(MonHoc monHoc) {
        return new CatalogDtos.MonHocDto(monHoc.getId(), monHoc.getMa(), monHoc.getTen(),
                monHoc.getSoTinChi(), monHoc.getKhoa().getId(), monHoc.getKhoa().getTen());
    }

    public CatalogDtos.MonTienQuyetDto monTienQuyet(MonTienQuyet lienKet) {
        MonHoc tienQuyet = lienKet.getMonTienQuyet();
        return new CatalogDtos.MonTienQuyetDto(lienKet.getId(), lienKet.getMonHoc().getId(),
                tienQuyet.getId(), tienQuyet.getMa(), tienQuyet.getTen(), tienQuyet.getSoTinChi());
    }

    public CatalogDtos.HocKyDto hocKy(HocKy hocKy) {
        return new CatalogDtos.HocKyDto(hocKy.getId(), hocKy.getTen(), hocKy.getNamHoc(),
                hocKy.getNgayBatDau(), hocKy.getNgayKetThuc(), hocKy.getDangKyTu(), hocKy.getDangKyDen(),
                hocKy.getSoTinChiToiThieu(), hocKy.getSoTinChiToiDa(), hocKy.dangMoDangKy());
    }

    public LopHocPhanDtos.LopHocPhanDto lopHocPhan(LopHocPhan lop) {
        return new LopHocPhanDtos.LopHocPhanDto(lop.getId(), lop.getMa(),
                lop.getMonHoc().getId(), lop.getMonHoc().getMa(), lop.getMonHoc().getTen(),
                lop.getMonHoc().getSoTinChi(), lop.getHocKy().getId(), lop.getHocKy().getTen(),
                lop.getGiangVien().getId(), lop.getGiangVien().getHoTen(), lop.getPhong(),
                lop.getThu(), lop.getTietBatDau(), lop.getSoTiet(), lop.getSiSoToiDa(),
                lop.getSiSoHienTai(), lop.getConLai());
    }

    public RegistrationDtos.DangKyDto dangKy(DangKyHocPhan dangKy, Diem diem) {
        LopHocPhan lop = dangKy.getLopHocPhan();
        return new RegistrationDtos.DangKyDto(dangKy.getId(), lop.getId(), lop.getMa(),
                lop.getMonHoc().getId(), lop.getMonHoc().getMa(), lop.getMonHoc().getTen(),
                lop.getMonHoc().getSoTinChi(), lop.getGiangVien().getHoTen(), lop.getPhong(),
                lop.getThu(), lop.getTietBatDau(), lop.getSoTiet(),
                lop.getHocKy().getId(), lop.getHocKy().getTen(), lop.getHocKy().getNamHoc(),
                dangKy.getThoiGianDangKy(), dangKy.getTrangThai().name(),
                diem != null ? diem.getDiemQuaTrinh() : null,
                diem != null ? diem.getDiemCuoiKy() : null,
                diem != null ? diem.getDiemTongKet() : null);
    }

    public RegistrationDtos.DiemDto diem(DangKyHocPhan dangKy, Diem diem) {
        return new RegistrationDtos.DiemDto(dangKy.getId(), dangKy.getSinhVien().getId(),
                dangKy.getSinhVien().getMa(), dangKy.getSinhVien().getHoTen(),
                dangKy.getSinhVien().getEmail(),
                diem != null ? diem.getDiemQuaTrinh() : null,
                diem != null ? diem.getDiemCuoiKy() : null,
                diem != null ? diem.getDiemTongKet() : null);
    }

    public RegistrationDtos.ThoiKhoaBieuDto thoiKhoaBieu(DangKyHocPhan dangKy) {
        LopHocPhan lop = dangKy.getLopHocPhan();
        return new RegistrationDtos.ThoiKhoaBieuDto(lop.getId(), lop.getMa(), lop.getMonHoc().getTen(),
                lop.getPhong(), lop.getThu(), lop.getTietBatDau(), lop.getSoTiet(),
                lop.getGiangVien().getHoTen());
    }

    public UserDtos.SinhVienDto sinhVien(SinhVien sinhVien) {
        return new UserDtos.SinhVienDto(sinhVien.getId(), sinhVien.getMa(), sinhVien.getHoTen(),
                sinhVien.getEmail(), sinhVien.getSoDienThoai(),
                sinhVien.getKhoa() != null ? sinhVien.getKhoa().getId() : null,
                sinhVien.getKhoa() != null ? sinhVien.getKhoa().getTen() : null,
                sinhVien.getNganh() != null ? sinhVien.getNganh().getId() : null,
                sinhVien.getNganh() != null ? sinhVien.getNganh().getTen() : null,
                sinhVien.getLopHanhChinh() != null ? sinhVien.getLopHanhChinh().getId() : null,
                sinhVien.getLopHanhChinh() != null ? sinhVien.getLopHanhChinh().getTen() : null,
                sinhVien.getUser().getUsername(), sinhVien.getUser().isTrangThai());
    }

    public UserDtos.GiangVienDto giangVien(GiangVien giangVien) {
        return new UserDtos.GiangVienDto(giangVien.getId(), giangVien.getMa(), giangVien.getHoTen(),
                giangVien.getEmail(), giangVien.getSoDienThoai(),
                giangVien.getKhoa() != null ? giangVien.getKhoa().getId() : null,
                giangVien.getKhoa() != null ? giangVien.getKhoa().getTen() : null,
                giangVien.getUser().getUsername(), giangVien.getUser().isTrangThai());
    }
}
