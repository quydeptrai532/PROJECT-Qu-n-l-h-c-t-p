package com.edu.registration.service;

import com.edu.registration.dto.LopHocPhanDtos;
import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.entity.GiangVien;
import com.edu.registration.entity.HocKy;
import com.edu.registration.entity.LopHocPhan;
import com.edu.registration.entity.MonHoc;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.DangKyHocPhanRepository;
import com.edu.registration.repository.GiangVienRepository;
import com.edu.registration.repository.LopHocPhanRepository;
import com.edu.registration.repository.MonHocRepository;
import com.edu.registration.repository.ThongBaoNghiRepository;
import com.edu.registration.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class LopHocPhanService {

    private final LopHocPhanRepository lopHocPhanRepository;
    private final MonHocRepository monHocRepository;
    private final GiangVienRepository giangVienRepository;
    private final DangKyHocPhanRepository dangKyHocPhanRepository;
    private final ThongBaoNghiRepository thongBaoNghiRepository;
    private final HocKyService hocKyService;
    private final CurrentUser currentUser;
    private final DtoMapper mapper;

    public LopHocPhanService(LopHocPhanRepository lopHocPhanRepository, MonHocRepository monHocRepository,
                             GiangVienRepository giangVienRepository,
                             DangKyHocPhanRepository dangKyHocPhanRepository,
                             ThongBaoNghiRepository thongBaoNghiRepository,
                             HocKyService hocKyService, CurrentUser currentUser, DtoMapper mapper) {
        this.lopHocPhanRepository = lopHocPhanRepository;
        this.monHocRepository = monHocRepository;
        this.giangVienRepository = giangVienRepository;
        this.dangKyHocPhanRepository = dangKyHocPhanRepository;
        this.thongBaoNghiRepository = thongBaoNghiRepository;
        this.hocKyService = hocKyService;
        this.currentUser = currentUser;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<LopHocPhanDtos.LopHocPhanDto> danhSach(Long hocKyId, String keyword) {
        HocKy hocKy = hocKyService.layHoacHienTai(hocKyId);
        List<LopHocPhan> danhSach = lopHocPhanRepository.findByHocKyIdOrderByMaAsc(hocKy.getId());
        if (keyword != null && !keyword.isBlank()) {
            String tuKhoa = keyword.trim().toLowerCase(Locale.ROOT);
            danhSach = danhSach.stream()
                    .filter(lop -> lop.getMa().toLowerCase(Locale.ROOT).contains(tuKhoa)
                            || lop.getMonHoc().getTen().toLowerCase(Locale.ROOT).contains(tuKhoa)
                            || lop.getMonHoc().getMa().toLowerCase(Locale.ROOT).contains(tuKhoa)
                            || lop.getGiangVien().getHoTen().toLowerCase(Locale.ROOT).contains(tuKhoa))
                    .toList();
        }
        return danhSach.stream().map(mapper::lopHocPhan).toList();
    }

    @Transactional(readOnly = true)
    public List<LopHocPhanDtos.LopHocPhanDto> danhSachTheoGiangVien(Long giangVienId, Long hocKyId) {
        if (hocKyId != null) {
            return lopHocPhanRepository.findByGiangVienIdAndHocKyIdOrderByMaAsc(giangVienId, hocKyId)
                    .stream().map(mapper::lopHocPhan).toList();
        }
        return lopHocPhanRepository.findByGiangVienIdOrderByMaAsc(giangVienId)
                .stream().map(mapper::lopHocPhan).toList();
    }

    @Transactional(readOnly = true)
    public List<LopHocPhanDtos.LopHocPhanDto> danhSachCuaToi(Long hocKyId) {
        GiangVien giangVien = giangVienRepository.findByUser(currentUser.get())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ giảng viên"));
        return danhSachTheoGiangVien(giangVien.getId(), hocKyId);
    }

    @Transactional(readOnly = true)
    public List<RegistrationDtos.ThoiKhoaBieuDto> thoiKhoaBieuCuaToi(Long hocKyId) {
        GiangVien giangVien = giangVienRepository.findByUser(currentUser.get())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ giảng viên"));
        HocKy hocKy = hocKyService.layHoacHienTai(hocKyId);
        List<LopHocPhan> danhSach = lopHocPhanRepository
                .findByGiangVienIdAndHocKyIdOrderByMaAsc(giangVien.getId(), hocKy.getId());
        return danhSach.stream()
                .map(lop -> new RegistrationDtos.ThoiKhoaBieuDto(lop.getId(), lop.getMa(),
                        lop.getMonHoc().getTen(), lop.getPhong(), lop.getThu(), lop.getTietBatDau(),
                        lop.getSoTiet(), lop.getGiangVien().getHoTen()))
                .toList();
    }

    @Transactional(readOnly = true)
    public LopHocPhanDtos.LopHocPhanDto chiTiet(Long id) {
        return mapper.lopHocPhan(lopHocPhanRepository.findWithDetailsById(id)
                .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại")));
    }

    public LopHocPhanDtos.LopHocPhanResponse tao(LopHocPhanDtos.LopHocPhanRequest request) {
        kiemTraLich(request);
        String ma = request.ma().trim();
        if (lopHocPhanRepository.existsByMa(ma)) {
            throw ApiException.conflict("Mã lớp học phần đã tồn tại");
        }
        MonHoc monHoc = monHocRepository.findById(request.monHocId())
                .orElseThrow(() -> ApiException.notFound("Môn học không tồn tại"));
        GiangVien giangVien = giangVienRepository.findById(request.giangVienId())
                .orElseThrow(() -> ApiException.notFound("Giảng viên không tồn tại"));
        HocKy hocKy = hocKyService.lay(request.hocKyId());

        List<String> canhBao = kiemTraCanhBao(request, null);

        LopHocPhan lop = new LopHocPhan();
        lop.setMa(ma);
        lop.setMonHoc(monHoc);
        lop.setHocKy(hocKy);
        lop.setGiangVien(giangVien);
        apDung(lop, request);
        lop.setSiSoHienTai(0);

        return new LopHocPhanDtos.LopHocPhanResponse(mapper.lopHocPhan(lopHocPhanRepository.save(lop)), canhBao);
    }

    public LopHocPhanDtos.LopHocPhanResponse sua(Long id, LopHocPhanDtos.LopHocPhanRequest request) {
        kiemTraLich(request);
        LopHocPhan lop = lopHocPhanRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));
        String ma = request.ma().trim();
        if (!lop.getMa().equalsIgnoreCase(ma) && lopHocPhanRepository.existsByMa(ma)) {
            throw ApiException.conflict("Mã lớp học phần đã tồn tại");
        }
        if (request.siSoToiDa() < lop.getSiSoHienTai()) {
            throw ApiException.badRequest("Sĩ số tối đa không được nhỏ hơn số sinh viên đã đăng ký ("
                    + lop.getSiSoHienTai() + ")");
        }
        MonHoc monHoc = monHocRepository.findById(request.monHocId())
                .orElseThrow(() -> ApiException.notFound("Môn học không tồn tại"));
        GiangVien giangVien = giangVienRepository.findById(request.giangVienId())
                .orElseThrow(() -> ApiException.notFound("Giảng viên không tồn tại"));
        HocKy hocKy = hocKyService.lay(request.hocKyId());

        List<String> canhBao = kiemTraCanhBao(request, id);

        lop.setMa(ma);
        lop.setMonHoc(monHoc);
        lop.setHocKy(hocKy);
        lop.setGiangVien(giangVien);
        apDung(lop, request);
        return new LopHocPhanDtos.LopHocPhanResponse(mapper.lopHocPhan(lop), canhBao);
    }

    public void xoa(Long id) {
        LopHocPhan lop = lopHocPhanRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));
        if (dangKyHocPhanRepository.existsByLopHocPhanId(id)) {
            throw ApiException.conflict("Lớp đã có sinh viên đăng ký, không thể xóa");
        }
        thongBaoNghiRepository.deleteByLopHocPhanId(id);
        lopHocPhanRepository.delete(lop);
    }

    private void kiemTraLich(LopHocPhanDtos.LopHocPhanRequest request) {
        if (request.tietBatDau() + request.soTiet() - 1 > 12) {
            throw ApiException.badRequest("Lịch học vượt quá tiết 12");
        }
    }

    private List<String> kiemTraCanhBao(LopHocPhanDtos.LopHocPhanRequest request, Long boQuaId) {
        LopHocPhan nhap = new LopHocPhan();
        nhap.setThu(request.thu());
        nhap.setTietBatDau(request.tietBatDau());
        nhap.setSoTiet(request.soTiet());

        List<String> canhBao = new ArrayList<>();
        for (LopHocPhan lop : lopHocPhanRepository.findByHocKyIdOrderByMaAsc(request.hocKyId())) {
            if (boQuaId != null && boQuaId.equals(lop.getId())) {
                continue;
            }
            if (!lop.trungLichVoi(nhap)) {
                continue;
            }
            if (lop.getPhong().equalsIgnoreCase(request.phong().trim())) {
                canhBao.add("Trùng phòng " + lop.getPhong() + " với lớp " + lop.getMa());
            }
            if (lop.getGiangVien().getId().equals(request.giangVienId())) {
                canhBao.add("Giảng viên đã có lớp " + lop.getMa() + " trong cùng khung giờ");
            }
        }
        return canhBao;
    }

    private void apDung(LopHocPhan lop, LopHocPhanDtos.LopHocPhanRequest request) {
        lop.setPhong(request.phong().trim());
        lop.setThu(request.thu());
        lop.setTietBatDau(request.tietBatDau());
        lop.setSoTiet(request.soTiet());
        lop.setSiSoToiDa(request.siSoToiDa());
    }
}
