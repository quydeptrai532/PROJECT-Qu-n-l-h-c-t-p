package com.edu.registration.service;

import com.edu.registration.dto.UserDtos;
import com.edu.registration.entity.GiangVien;
import com.edu.registration.entity.Khoa;
import com.edu.registration.entity.LopHanhChinh;
import com.edu.registration.entity.Nganh;
import com.edu.registration.entity.Role;
import com.edu.registration.entity.SinhVien;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.GiangVienRepository;
import com.edu.registration.repository.KhoaRepository;
import com.edu.registration.repository.LopHanhChinhRepository;
import com.edu.registration.repository.NganhRepository;
import com.edu.registration.repository.SinhVienRepository;
import com.edu.registration.repository.UserRepository;
import com.edu.registration.security.UserAuthCache;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    private static final String MAT_KHAU_MAC_DINH = "123456";

    private final UserRepository userRepository;
    private final SinhVienRepository sinhVienRepository;
    private final GiangVienRepository giangVienRepository;
    private final KhoaRepository khoaRepository;
    private final NganhRepository nganhRepository;
    private final LopHanhChinhRepository lopHanhChinhRepository;
    private final PasswordEncoder passwordEncoder;
    private final DtoMapper mapper;
    private final TransactionTemplate transactionTemplate;
    private final UserAuthCache userAuthCache;

    public UserService(UserRepository userRepository, SinhVienRepository sinhVienRepository,
                       GiangVienRepository giangVienRepository, KhoaRepository khoaRepository,
                       NganhRepository nganhRepository, LopHanhChinhRepository lopHanhChinhRepository,
                       PasswordEncoder passwordEncoder, DtoMapper mapper, TransactionTemplate transactionTemplate,
                       UserAuthCache userAuthCache) {
        this.userRepository = userRepository;
        this.sinhVienRepository = sinhVienRepository;
        this.giangVienRepository = giangVienRepository;
        this.khoaRepository = khoaRepository;
        this.nganhRepository = nganhRepository;
        this.lopHanhChinhRepository = lopHanhChinhRepository;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
        this.transactionTemplate = transactionTemplate;
        this.userAuthCache = userAuthCache;
    }

    @Transactional(readOnly = true)
    public List<UserDtos.SinhVienDto> danhSachSinhVien(String keyword) {
        List<SinhVien> danhSach = (keyword == null || keyword.isBlank())
                ? sinhVienRepository.findAllByOrderByMaAsc()
                : sinhVienRepository.findByHoTenContainingIgnoreCaseOrMaContainingIgnoreCaseOrderByMaAsc(
                        keyword.trim(), keyword.trim());
        return danhSach.stream().map(mapper::sinhVien).toList();
    }

    @Transactional
    public UserDtos.SinhVienDto taoSinhVien(UserDtos.SinhVienRequest request) {
        return taoSinhVienTuRecord(request.ma(), request.hoTen(), request.email(), request.soDienThoai(),
                request.khoaId(), request.nganhId(), request.lopId(), request.username(), request.matKhau());
    }

    @Transactional
    public UserDtos.SinhVienDto suaSinhVien(Long id, UserDtos.SinhVienRequest request) {
        SinhVien sinhVien = sinhVienRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Sinh viên không tồn tại"));
        String ma = request.ma().trim();
        if (!sinhVien.getMa().equalsIgnoreCase(ma) && sinhVienRepository.existsByMa(ma)) {
            throw ApiException.conflict("Mã sinh viên đã tồn tại");
        }
        sinhVien.setMa(ma);
        sinhVien.setHoTen(request.hoTen().trim());
        sinhVien.setEmail(request.email());
        sinhVien.setSoDienThoai(request.soDienThoai());
        sinhVien.setKhoa(request.khoaId() != null ? timKhoa(request.khoaId()) : null);
        sinhVien.setNganh(request.nganhId() != null ? timNganh(request.nganhId()) : null);
        sinhVien.setLopHanhChinh(request.lopId() != null ? timLop(request.lopId()) : null);
        if (request.matKhau() != null && !request.matKhau().isBlank()) {
            sinhVien.getUser().setPassword(passwordEncoder.encode(request.matKhau()));
        }
        return mapper.sinhVien(sinhVien);
    }

    @Transactional
    public UserDtos.SinhVienDto doiTrangThaiSinhVien(Long id, boolean trangThai) {
        SinhVien sinhVien = sinhVienRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Sinh viên không tồn tại"));
        sinhVien.getUser().setTrangThai(trangThai);
        userAuthCache.evict(sinhVien.getUser().getUsername());
        return mapper.sinhVien(sinhVien);
    }

    @Transactional(readOnly = true)
    public List<UserDtos.GiangVienDto> danhSachGiangVien(String keyword) {
        List<GiangVien> danhSach = (keyword == null || keyword.isBlank())
                ? giangVienRepository.findAllByOrderByMaAsc()
                : giangVienRepository.findByHoTenContainingIgnoreCaseOrMaContainingIgnoreCaseOrderByMaAsc(
                        keyword.trim(), keyword.trim());
        return danhSach.stream().map(mapper::giangVien).toList();
    }

    @Transactional
    public UserDtos.GiangVienDto taoGiangVien(UserDtos.GiangVienRequest request) {
        String ma = request.ma().trim();
        if (giangVienRepository.existsByMa(ma)) {
            throw ApiException.conflict("Mã giảng viên đã tồn tại");
        }
        User user = taoUser(request.username(), request.matKhau(), Role.GIANG_VIEN);
        GiangVien giangVien = new GiangVien();
        giangVien.setUser(user);
        giangVien.setMa(ma);
        giangVien.setHoTen(request.hoTen().trim());
        giangVien.setEmail(request.email());
        giangVien.setSoDienThoai(request.soDienThoai());
        giangVien.setKhoa(request.khoaId() != null ? timKhoa(request.khoaId()) : null);
        return mapper.giangVien(giangVienRepository.save(giangVien));
    }

    @Transactional
    public UserDtos.GiangVienDto suaGiangVien(Long id, UserDtos.GiangVienRequest request) {
        GiangVien giangVien = giangVienRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Giảng viên không tồn tại"));
        String ma = request.ma().trim();
        if (!giangVien.getMa().equalsIgnoreCase(ma) && giangVienRepository.existsByMa(ma)) {
            throw ApiException.conflict("Mã giảng viên đã tồn tại");
        }
        giangVien.setMa(ma);
        giangVien.setHoTen(request.hoTen().trim());
        giangVien.setEmail(request.email());
        giangVien.setSoDienThoai(request.soDienThoai());
        giangVien.setKhoa(request.khoaId() != null ? timKhoa(request.khoaId()) : null);
        if (request.matKhau() != null && !request.matKhau().isBlank()) {
            giangVien.getUser().setPassword(passwordEncoder.encode(request.matKhau()));
        }
        return mapper.giangVien(giangVien);
    }

    @Transactional
    public UserDtos.GiangVienDto doiTrangThaiGiangVien(Long id, boolean trangThai) {
        GiangVien giangVien = giangVienRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Giảng viên không tồn tại"));
        giangVien.getUser().setTrangThai(trangThai);
        userAuthCache.evict(giangVien.getUser().getUsername());
        return mapper.giangVien(giangVien);
    }

    public UserDtos.ImportResultDto importSinhVien(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Vui lòng chọn file CSV");
        }

        List<String> loi = new ArrayList<>();
        int thanhCong = 0;

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setTrim(true)
                     .setIgnoreEmptyLines(true)
                     .build()
                     .parse(reader)) {

            for (CSVRecord record : parser) {
                if (record.getRecordNumber() == 1 && "ma".equalsIgnoreCase(record.get(0))) {
                    continue;
                }
                try {
                    if (record.size() < 4) {
                        throw ApiException.badRequest("Cần tối thiểu 4 cột: ma,hoTen,email,khoaId");
                    }
                    transactionTemplate.executeWithoutResult(status -> taoSinhVienTuRecord(
                            record.get(0), record.get(1), record.get(2), null,
                            parseLong(record, 3), parseLong(record, 4), parseLong(record, 5),
                            record.get(0), MAT_KHAU_MAC_DINH));
                    thanhCong++;
                } catch (ApiException ex) {
                    loi.add("Dòng " + record.getRecordNumber() + ": " + ex.getMessage());
                } catch (RuntimeException ex) {
                    loi.add("Dòng " + record.getRecordNumber() + ": dữ liệu không hợp lệ");
                }
            }
        } catch (IOException ex) {
            throw ApiException.badRequest("Không đọc được file CSV");
        }

        return new UserDtos.ImportResultDto(thanhCong, loi);
    }

    private UserDtos.SinhVienDto taoSinhVienTuRecord(String ma, String hoTen, String email, String soDienThoai,
                                                     Long khoaId, Long nganhId, Long lopId,
                                                     String username, String matKhau) {
        if (ma == null || ma.isBlank()) {
            throw ApiException.badRequest("Mã sinh viên không được để trống");
        }
        if (hoTen == null || hoTen.isBlank()) {
            throw ApiException.badRequest("Họ tên không được để trống");
        }
        if (username == null || username.isBlank()) {
            throw ApiException.badRequest("Tên đăng nhập không được để trống");
        }
        String maChuan = ma.trim();
        if (sinhVienRepository.existsByMa(maChuan)) {
            throw ApiException.conflict("Mã sinh viên đã tồn tại: " + maChuan);
        }

        User user = taoUser(username, matKhau, Role.SINH_VIEN);
        SinhVien sinhVien = new SinhVien();
        sinhVien.setUser(user);
        sinhVien.setMa(maChuan);
        sinhVien.setHoTen(hoTen.trim());
        sinhVien.setEmail(email);
        sinhVien.setSoDienThoai(soDienThoai);
        sinhVien.setKhoa(khoaId != null ? timKhoa(khoaId) : null);
        sinhVien.setNganh(nganhId != null ? timNganh(nganhId) : null);
        sinhVien.setLopHanhChinh(lopId != null ? timLop(lopId) : null);
        return mapper.sinhVien(sinhVienRepository.save(sinhVien));
    }

    private User taoUser(String username, String matKhau, Role role) {
        String tenDangNhap = username.trim();
        if (userRepository.existsByUsername(tenDangNhap)) {
            throw ApiException.conflict("Tên đăng nhập đã tồn tại: " + tenDangNhap);
        }
        User user = new User();
        user.setUsername(tenDangNhap);
        user.setPassword(passwordEncoder.encode(
                matKhau == null || matKhau.isBlank() ? MAT_KHAU_MAC_DINH : matKhau));
        user.setRole(role);
        user.setTrangThai(true);
        return userRepository.save(user);
    }

    private Long parseLong(CSVRecord record, int index) {
        if (record.size() <= index) {
            return null;
        }
        String value = record.get(index);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            throw ApiException.badRequest("Giá trị số không hợp lệ: " + value);
        }
    }

    private Khoa timKhoa(Long id) {
        return khoaRepository.findById(id).orElseThrow(() -> ApiException.notFound("Khoa không tồn tại"));
    }

    private Nganh timNganh(Long id) {
        return nganhRepository.findById(id).orElseThrow(() -> ApiException.notFound("Ngành không tồn tại"));
    }

    private LopHanhChinh timLop(Long id) {
        return lopHanhChinhRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Lớp hành chính không tồn tại"));
    }
}
