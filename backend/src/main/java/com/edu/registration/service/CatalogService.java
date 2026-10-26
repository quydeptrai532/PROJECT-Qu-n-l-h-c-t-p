package com.edu.registration.service;

import com.edu.registration.dto.CatalogDtos;
import com.edu.registration.entity.HocKy;
import com.edu.registration.entity.Khoa;
import com.edu.registration.entity.LopHanhChinh;
import com.edu.registration.entity.MonHoc;
import com.edu.registration.entity.MonTienQuyet;
import com.edu.registration.entity.Nganh;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.HocKyRepository;
import com.edu.registration.repository.KhoaRepository;
import com.edu.registration.repository.LopHanhChinhRepository;
import com.edu.registration.repository.LopHocPhanRepository;
import com.edu.registration.repository.MonHocRepository;
import com.edu.registration.repository.MonTienQuyetRepository;
import com.edu.registration.repository.NganhRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CatalogService {

    private final KhoaRepository khoaRepository;
    private final NganhRepository nganhRepository;
    private final LopHanhChinhRepository lopHanhChinhRepository;
    private final MonHocRepository monHocRepository;
    private final MonTienQuyetRepository monTienQuyetRepository;
    private final HocKyRepository hocKyRepository;
    private final LopHocPhanRepository lopHocPhanRepository;
    private final DtoMapper mapper;

    public CatalogService(KhoaRepository khoaRepository, NganhRepository nganhRepository,
                          LopHanhChinhRepository lopHanhChinhRepository, MonHocRepository monHocRepository,
                          MonTienQuyetRepository monTienQuyetRepository, HocKyRepository hocKyRepository,
                          LopHocPhanRepository lopHocPhanRepository, DtoMapper mapper) {
        this.khoaRepository = khoaRepository;
        this.nganhRepository = nganhRepository;
        this.lopHanhChinhRepository = lopHanhChinhRepository;
        this.monHocRepository = monHocRepository;
        this.monTienQuyetRepository = monTienQuyetRepository;
        this.hocKyRepository = hocKyRepository;
        this.lopHocPhanRepository = lopHocPhanRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.KhoaDto> danhSachKhoa() {
        return khoaRepository.findAllByOrderByMaAsc().stream().map(mapper::khoa).toList();
    }

    public CatalogDtos.KhoaDto taoKhoa(CatalogDtos.KhoaRequest request) {
        String ma = request.ma().trim();
        if (khoaRepository.existsByMa(ma)) {
            throw ApiException.conflict("Mã khoa đã tồn tại");
        }
        Khoa khoa = new Khoa();
        khoa.setMa(ma);
        khoa.setTen(request.ten().trim());
        return mapper.khoa(khoaRepository.save(khoa));
    }

    public CatalogDtos.KhoaDto suaKhoa(Long id, CatalogDtos.KhoaRequest request) {
        Khoa khoa = khoaRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Khoa không tồn tại"));
        String ma = request.ma().trim();
        if (!khoa.getMa().equalsIgnoreCase(ma) && khoaRepository.existsByMa(ma)) {
            throw ApiException.conflict("Mã khoa đã tồn tại");
        }
        khoa.setMa(ma);
        khoa.setTen(request.ten().trim());
        return mapper.khoa(khoa);
    }

    public void xoaKhoa(Long id) {
        khoaRepository.delete(khoaRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Khoa không tồn tại")));
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.NganhDto> danhSachNganh() {
        return nganhRepository.findAllByOrderByMaAsc().stream().map(mapper::nganh).toList();
    }

    public CatalogDtos.NganhDto taoNganh(CatalogDtos.NganhRequest request) {
        String ma = request.ma().trim();
        if (nganhRepository.existsByMa(ma)) {
            throw ApiException.conflict("Mã ngành đã tồn tại");
        }
        Nganh nganh = new Nganh();
        nganh.setMa(ma);
        nganh.setTen(request.ten().trim());
        nganh.setKhoa(layKhoa(request.khoaId()));
        return mapper.nganh(nganhRepository.save(nganh));
    }

    public CatalogDtos.NganhDto suaNganh(Long id, CatalogDtos.NganhRequest request) {
        Nganh nganh = nganhRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Ngành không tồn tại"));
        String ma = request.ma().trim();
        if (!nganh.getMa().equalsIgnoreCase(ma) && nganhRepository.existsByMa(ma)) {
            throw ApiException.conflict("Mã ngành đã tồn tại");
        }
        nganh.setMa(ma);
        nganh.setTen(request.ten().trim());
        nganh.setKhoa(layKhoa(request.khoaId()));
        return mapper.nganh(nganh);
    }

    public void xoaNganh(Long id) {
        nganhRepository.delete(nganhRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Ngành không tồn tại")));
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.LopHanhChinhDto> danhSachLopHanhChinh() {
        return lopHanhChinhRepository.findAllByOrderByMaAsc().stream().map(mapper::lopHanhChinh).toList();
    }

    public CatalogDtos.LopHanhChinhDto taoLopHanhChinh(CatalogDtos.LopHanhChinhRequest request) {
        String ma = request.ma().trim();
        if (lopHanhChinhRepository.existsByMa(ma)) {
            throw ApiException.conflict("Mã lớp hành chính đã tồn tại");
        }
        LopHanhChinh lop = new LopHanhChinh();
        lop.setMa(ma);
        lop.setTen(request.ten().trim());
        lop.setNganh(layNganh(request.nganhId()));
        return mapper.lopHanhChinh(lopHanhChinhRepository.save(lop));
    }

    public CatalogDtos.LopHanhChinhDto suaLopHanhChinh(Long id, CatalogDtos.LopHanhChinhRequest request) {
        LopHanhChinh lop = lopHanhChinhRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Lớp hành chính không tồn tại"));
        String ma = request.ma().trim();
        if (!lop.getMa().equalsIgnoreCase(ma) && lopHanhChinhRepository.existsByMa(ma)) {
            throw ApiException.conflict("Mã lớp hành chính đã tồn tại");
        }
        lop.setMa(ma);
        lop.setTen(request.ten().trim());
        lop.setNganh(layNganh(request.nganhId()));
        return mapper.lopHanhChinh(lop);
    }

    public void xoaLopHanhChinh(Long id) {
        lopHanhChinhRepository.delete(lopHanhChinhRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Lớp hành chính không tồn tại")));
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.MonHocDto> danhSachMonHoc() {
        return monHocRepository.findAllByOrderByMaAsc().stream().map(mapper::monHoc).toList();
    }

    public CatalogDtos.MonHocDto taoMonHoc(CatalogDtos.MonHocRequest request) {
        String ma = request.ma().trim();
        if (monHocRepository.existsByMa(ma)) {
            throw ApiException.conflict("Mã môn học đã tồn tại");
        }
        MonHoc monHoc = new MonHoc();
        monHoc.setMa(ma);
        monHoc.setTen(request.ten().trim());
        monHoc.setSoTinChi(request.soTinChi());
        monHoc.setKhoa(layKhoa(request.khoaId()));
        return mapper.monHoc(monHocRepository.save(monHoc));
    }

    public CatalogDtos.MonHocDto suaMonHoc(Long id, CatalogDtos.MonHocRequest request) {
        MonHoc monHoc = monHocRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Môn học không tồn tại"));
        String ma = request.ma().trim();
        if (!monHoc.getMa().equalsIgnoreCase(ma) && monHocRepository.existsByMa(ma)) {
            throw ApiException.conflict("Mã môn học đã tồn tại");
        }
        monHoc.setMa(ma);
        monHoc.setTen(request.ten().trim());
        monHoc.setSoTinChi(request.soTinChi());
        monHoc.setKhoa(layKhoa(request.khoaId()));
        return mapper.monHoc(monHoc);
    }

    public void xoaMonHoc(Long id) {
        monHocRepository.delete(monHocRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Môn học không tồn tại")));
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.MonTienQuyetDto> danhSachTienQuyet(Long monHocId) {
        layMonHoc(monHocId);
        return monTienQuyetRepository.findByMonHocIdOrderByIdAsc(monHocId).stream()
                .map(mapper::monTienQuyet).toList();
    }

    public CatalogDtos.MonTienQuyetDto themTienQuyet(Long monHocId, CatalogDtos.MonTienQuyetRequest request) {
        MonHoc monHoc = layMonHoc(monHocId);
        MonHoc tienQuyet = layMonHoc(request.monTienQuyetId());
        if (monHoc.getId().equals(tienQuyet.getId())) {
            throw ApiException.badRequest("Môn học không thể là tiên quyết của chính nó");
        }
        if (monTienQuyetRepository.existsByMonHocIdAndMonTienQuyetId(monHocId, tienQuyet.getId())) {
            throw ApiException.conflict("Môn tiên quyết đã tồn tại");
        }
        if (monTienQuyetRepository.existsByMonHocIdAndMonTienQuyetId(tienQuyet.getId(), monHocId)) {
            throw ApiException.badRequest("Tạo thành vòng lặp tiên quyết giữa hai môn học");
        }
        MonTienQuyet lienKet = monTienQuyetRepository.save(new MonTienQuyet(monHoc, tienQuyet));
        return mapper.monTienQuyet(lienKet);
    }

    public void xoaTienQuyet(Long monHocId, Long lienKetId) {
        MonTienQuyet lienKet = monTienQuyetRepository.findById(lienKetId)
                .orElseThrow(() -> ApiException.notFound("Liên kết môn tiên quyết không tồn tại"));
        if (!lienKet.getMonHoc().getId().equals(monHocId)) {
            throw ApiException.badRequest("Liên kết không thuộc môn học này");
        }
        monTienQuyetRepository.delete(lienKet);
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.HocKyDto> danhSachHocKy() {
        return hocKyRepository.findAllByOrderByNgayBatDauDesc().stream().map(mapper::hocKy).toList();
    }

    public CatalogDtos.HocKyDto taoHocKy(CatalogDtos.HocKyRequest request) {
        kiemTraHocKy(request);
        if (hocKyRepository.existsByTenAndNamHoc(request.ten().trim(), request.namHoc().trim())) {
            throw ApiException.conflict("Học kỳ đã tồn tại trong năm học này");
        }
        HocKy hocKy = new HocKy();
        apDungHocKy(hocKy, request);
        return mapper.hocKy(hocKyRepository.save(hocKy));
    }

    public CatalogDtos.HocKyDto suaHocKy(Long id, CatalogDtos.HocKyRequest request) {
        HocKy hocKy = hocKyRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Học kỳ không tồn tại"));
        kiemTraHocKy(request);
        apDungHocKy(hocKy, request);
        return mapper.hocKy(hocKy);
    }

    public void xoaHocKy(Long id) {
        HocKy hocKy = hocKyRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Học kỳ không tồn tại"));
        if (lopHocPhanRepository.countByHocKyId(id) > 0) {
            throw ApiException.conflict("Học kỳ đã có lớp học phần, không thể xóa");
        }
        hocKyRepository.delete(hocKy);
    }

    private void kiemTraHocKy(CatalogDtos.HocKyRequest request) {
        if (request.ngayBatDau().isAfter(request.ngayKetThuc())) {
            throw ApiException.badRequest("Ngày bắt đầu phải trước ngày kết thúc");
        }
        if (request.dangKyTu().isAfter(request.dangKyDen())) {
            throw ApiException.badRequest("Ngày mở đăng ký phải trước ngày đóng đăng ký");
        }
        if (request.soTinChiToiThieu() > request.soTinChiToiDa()) {
            throw ApiException.badRequest("Số tín chỉ tối thiểu không được lớn hơn số tín chỉ tối đa");
        }
    }

    private void apDungHocKy(HocKy hocKy, CatalogDtos.HocKyRequest request) {
        hocKy.setTen(request.ten().trim());
        hocKy.setNamHoc(request.namHoc().trim());
        hocKy.setNgayBatDau(request.ngayBatDau());
        hocKy.setNgayKetThuc(request.ngayKetThuc());
        hocKy.setDangKyTu(request.dangKyTu());
        hocKy.setDangKyDen(request.dangKyDen());
        hocKy.setSoTinChiToiThieu(request.soTinChiToiThieu());
        hocKy.setSoTinChiToiDa(request.soTinChiToiDa());
    }

    private Khoa layKhoa(Long id) {
        return khoaRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Khoa không tồn tại"));
    }

    private Nganh layNganh(Long id) {
        return nganhRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Ngành không tồn tại"));
    }

    private MonHoc layMonHoc(Long id) {
        return monHocRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Môn học không tồn tại"));
    }
}
