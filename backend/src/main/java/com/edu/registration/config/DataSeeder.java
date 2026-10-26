package com.edu.registration.config;

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
import com.edu.registration.entity.Role;
import com.edu.registration.entity.SinhVien;
import com.edu.registration.entity.TrangThaiDangKy;
import com.edu.registration.entity.User;
import com.edu.registration.repository.DangKyHocPhanRepository;
import com.edu.registration.repository.DiemRepository;
import com.edu.registration.repository.GiangVienRepository;
import com.edu.registration.repository.HocKyRepository;
import com.edu.registration.repository.KhoaRepository;
import com.edu.registration.repository.LopHanhChinhRepository;
import com.edu.registration.repository.LopHocPhanRepository;
import com.edu.registration.repository.MonHocRepository;
import com.edu.registration.repository.MonTienQuyetRepository;
import com.edu.registration.repository.NganhRepository;
import com.edu.registration.repository.SinhVienRepository;
import com.edu.registration.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final KhoaRepository khoaRepository;
    private final NganhRepository nganhRepository;
    private final LopHanhChinhRepository lopHanhChinhRepository;
    private final MonHocRepository monHocRepository;
    private final MonTienQuyetRepository monTienQuyetRepository;
    private final HocKyRepository hocKyRepository;
    private final GiangVienRepository giangVienRepository;
    private final SinhVienRepository sinhVienRepository;
    private final LopHocPhanRepository lopHocPhanRepository;
    private final DangKyHocPhanRepository dangKyHocPhanRepository;
    private final DiemRepository diemRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, KhoaRepository khoaRepository,
                      NganhRepository nganhRepository, LopHanhChinhRepository lopHanhChinhRepository,
                      MonHocRepository monHocRepository, MonTienQuyetRepository monTienQuyetRepository,
                      HocKyRepository hocKyRepository, GiangVienRepository giangVienRepository,
                      SinhVienRepository sinhVienRepository, LopHocPhanRepository lopHocPhanRepository,
                      DangKyHocPhanRepository dangKyHocPhanRepository, DiemRepository diemRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.khoaRepository = khoaRepository;
        this.nganhRepository = nganhRepository;
        this.lopHanhChinhRepository = lopHanhChinhRepository;
        this.monHocRepository = monHocRepository;
        this.monTienQuyetRepository = monTienQuyetRepository;
        this.hocKyRepository = hocKyRepository;
        this.giangVienRepository = giangVienRepository;
        this.sinhVienRepository = sinhVienRepository;
        this.lopHocPhanRepository = lopHocPhanRepository;
        this.dangKyHocPhanRepository = dangKyHocPhanRepository;
        this.diemRepository = diemRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Dữ liệu đã tồn tại, bỏ qua bước tạo dữ liệu mẫu");
            return;
        }

        Khoa cntt = taoKhoa("CNTT", "Công nghệ thông tin");
        Khoa kt = taoKhoa("KT", "Kinh tế");

        Nganh cnpm = taoNganh("CNPM", "Kỹ thuật phần mềm", cntt);
        Nganh httt = taoNganh("HTTT", "Hệ thống thông tin", cntt);
        Nganh ktqt = taoNganh("KTQT", "Kế toán", kt);

        LopHanhChinh cnpm01 = taoLop("CNPM01", "Kỹ thuật phần mềm 01", cnpm);
        LopHanhChinh cnpm02 = taoLop("CNPM02", "Kỹ thuật phần mềm 02", cnpm);
        LopHanhChinh httt01 = taoLop("HTTT01", "Hệ thống thông tin 01", httt);

        MonHoc int101 = taoMon("INT101", "Lập trình cơ bản", 3, cntt);
        MonHoc int201 = taoMon("INT201", "Cấu trúc dữ liệu và giải thuật", 3, cntt);
        MonHoc int301 = taoMon("INT301", "Cơ sở dữ liệu", 3, cntt);
        MonHoc int302 = taoMon("INT302", "Lập trình web", 3, cntt);
        MonHoc int303 = taoMon("INT303", "Mạng máy tính", 3, cntt);
        MonHoc int304 = taoMon("INT304", "Trí tuệ nhân tạo", 3, cntt);
        MonHoc mat101 = taoMon("MAT101", "Toán rời rạc", 3, cntt);
        MonHoc eco101 = taoMon("ECO101", "Kinh tế vi mô", 3, kt);

        taoTienQuyet(int201, int101);
        taoTienQuyet(int301, int201);
        taoTienQuyet(int302, int301);
        taoTienQuyet(int304, int201);

        LocalDate homNay = LocalDate.now();
        HocKy hocKyCu2 = taoHocKy("Học kỳ 1", "2025-2026",
                homNay.minusMonths(14), homNay.minusMonths(9),
                homNay.minusMonths(15), homNay.minusMonths(14).plusDays(20), 12, 24);
        HocKy hocKyCu = taoHocKy("Học kỳ 2", "2025-2026",
                homNay.minusMonths(8), homNay.minusMonths(4),
                homNay.minusMonths(9), homNay.minusMonths(8).plusDays(20), 12, 24);
        HocKy hocKyHienTai = taoHocKy("Học kỳ 1", "2026-2027",
                homNay.minusDays(14), homNay.plusDays(90),
                homNay.minusDays(5), homNay.plusDays(20), 12, 24);

        GiangVien gv01 = taoGiangVien("gv01", "TS. Nguyễn Văn An", "an.nv@university.edu.vn", cntt);
        GiangVien gv02 = taoGiangVien("gv02", "ThS. Trần Thị Bình", "binh.tt@university.edu.vn", cntt);
        GiangVien gv03 = taoGiangVien("gv03", "ThS. Lê Hoàng Cường", "cuong.lh@university.edu.vn", kt);

        SinhVien sv01 = taoSinhVien("sv01", "SV001", "Trần Minh Anh", "anh.tm@student.edu.vn",
                cntt, cnpm, cnpm01);
        SinhVien sv02 = taoSinhVien("sv02", "SV002", "Phạm Thu Hà", "ha.pt@student.edu.vn",
                cntt, cnpm, cnpm01);
        SinhVien sv03 = taoSinhVien("sv03", "SV003", "Đỗ Quang Huy", "huy.dq@student.edu.vn",
                cntt, cnpm, cnpm02);
        SinhVien sv04 = taoSinhVien("sv04", "SV004", "Vũ Ngọc Lan", "lan.vn@student.edu.vn",
                cntt, httt, httt01);
        SinhVien sv05 = taoSinhVien("sv05", "SV005", "Hoàng Gia Bảo", "bao.hg@student.edu.vn",
                cntt, httt, httt01);

        taoUser("admin", "admin123", Role.QUAN_TRI);

        LopHocPhan lopCu = taoLopHocPhan("INT101.01-HK2", int101, hocKyCu, gv01, "A101", 2, 1, 3, 60);
        dangKyVoiDiem(sv01, lopCu, 8.5, 9.0);
        dangKyVoiDiem(sv02, lopCu, 7.0, 8.0);
        dangKyVoiDiem(sv03, lopCu, 3.5, 4.0);

        LopHocPhan lopMatCu = taoLopHocPhan("MAT101.01-HK1", mat101, hocKyCu2, gv02, "A105", 4, 7, 3, 40);
        LopHocPhan lopEcoCu = taoLopHocPhan("ECO101.01-HK1", eco101, hocKyCu2, gv03, "C301", 3, 4, 3, 50);
        dangKyVoiDiem(sv01, lopMatCu, 7.0, 8.0);
        dangKyVoiDiem(sv01, lopEcoCu, 6.5, 7.0);
        dangKyVoiDiem(sv02, lopMatCu, 8.0, 8.5);
        dangKyVoiDiem(sv03, lopEcoCu, 5.0, 6.0);

        LopHocPhan int20101 = taoLopHocPhan("INT201.01", int201, hocKyHienTai, gv01, "A101", 2, 1, 3, 40);
        LopHocPhan int20102 = taoLopHocPhan("INT201.02", int201, hocKyHienTai, gv02, "A102", 4, 4, 3, 40);
        taoLopHocPhan("INT301.01", int301, hocKyHienTai, gv01, "B201", 3, 1, 3, 35);
        taoLopHocPhan("INT302.01", int302, hocKyHienTai, gv02, "A103", 5, 1, 3, 30);
        LopHocPhan int30301 = taoLopHocPhan("INT303.01", int303, hocKyHienTai, gv01, "B202", 6, 4, 3, 45);
        taoLopHocPhan("INT304.01", int304, hocKyHienTai, gv02, "A104", 7, 1, 3, 25);
        LopHocPhan ecoLop = taoLopHocPhan("ECO101.01", eco101, hocKyHienTai, gv03, "C301", 3, 4, 3, 50);
        LopHocPhan matLop = taoLopHocPhan("MAT101.01", mat101, hocKyHienTai, gv02, "A105", 4, 7, 3, 40);

        dangKyHienTai(sv01, int30301);
        dangKyHienTai(sv01, int20101);
        dangKyHienTai(sv01, matLop);
        dangKyHienTai(sv01, ecoLop);

        dangKyHienTai(sv03, int30301);
        dangKyHienTai(sv03, matLop);
        dangKyHienTai(sv03, ecoLop);
        dangKyHienTai(sv04, int30301);
        dangKyHienTai(sv04, matLop);
        dangKyHienTai(sv04, ecoLop);
        dangKyHienTai(sv05, int30301);
        dangKyHienTai(sv05, matLop);
        dangKyHienTai(sv05, ecoLop);

        for (int i = 6; i <= 20; i++) {
            String username = "sv" + String.format("%02d", i);
            String ma = String.format("SV%03d", i);
            SinhVien sv = taoSinhVien(username, ma, "Sinh viên " + i,
                    username + "@student.edu.vn", cntt, cnpm, cnpm01);
            dangKyVoiDiem(sv, lopCu, 6.5 + (i % 3), 7.0 + (i % 2));
            dangKyHienTai(sv, i % 2 == 0 ? int20102 : int20101);
            dangKyHienTai(sv, int30301);
            dangKyHienTai(sv, matLop);
            dangKyHienTai(sv, ecoLop);
        }

        log.info("Đã tạo dữ liệu mẫu: admin/admin123, gv01/123456, sv01..sv20/123456");
    }

    private User taoUser(String username, String matKhau, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(matKhau));
        user.setRole(role);
        user.setTrangThai(true);
        return userRepository.save(user);
    }

    private Khoa taoKhoa(String ma, String ten) {
        Khoa khoa = new Khoa();
        khoa.setMa(ma);
        khoa.setTen(ten);
        return khoaRepository.save(khoa);
    }

    private Nganh taoNganh(String ma, String ten, Khoa khoa) {
        Nganh nganh = new Nganh();
        nganh.setMa(ma);
        nganh.setTen(ten);
        nganh.setKhoa(khoa);
        return nganhRepository.save(nganh);
    }

    private LopHanhChinh taoLop(String ma, String ten, Nganh nganh) {
        LopHanhChinh lop = new LopHanhChinh();
        lop.setMa(ma);
        lop.setTen(ten);
        lop.setNganh(nganh);
        return lopHanhChinhRepository.save(lop);
    }

    private MonHoc taoMon(String ma, String ten, int soTinChi, Khoa khoa) {
        MonHoc monHoc = new MonHoc();
        monHoc.setMa(ma);
        monHoc.setTen(ten);
        monHoc.setSoTinChi(soTinChi);
        monHoc.setKhoa(khoa);
        return monHocRepository.save(monHoc);
    }

    private void taoTienQuyet(MonHoc monHoc, MonHoc tienQuyet) {
        monTienQuyetRepository.save(new MonTienQuyet(monHoc, tienQuyet));
    }

    private HocKy taoHocKy(String ten, String namHoc, LocalDate batDau, LocalDate ketThuc,
                           LocalDate dangKyTu, LocalDate dangKyDen, int min, int max) {
        HocKy hocKy = new HocKy();
        hocKy.setTen(ten);
        hocKy.setNamHoc(namHoc);
        hocKy.setNgayBatDau(batDau);
        hocKy.setNgayKetThuc(ketThuc);
        hocKy.setDangKyTu(dangKyTu);
        hocKy.setDangKyDen(dangKyDen);
        hocKy.setSoTinChiToiThieu(min);
        hocKy.setSoTinChiToiDa(max);
        return hocKyRepository.save(hocKy);
    }

    private GiangVien taoGiangVien(String username, String hoTen, String email, Khoa khoa) {
        User user = taoUser(username, "123456", Role.GIANG_VIEN);
        GiangVien giangVien = new GiangVien();
        giangVien.setUser(user);
        giangVien.setMa(username.toUpperCase());
        giangVien.setHoTen(hoTen);
        giangVien.setEmail(email);
        giangVien.setKhoa(khoa);
        return giangVienRepository.save(giangVien);
    }

    private SinhVien taoSinhVien(String username, String ma, String hoTen, String email,
                                 Khoa khoa, Nganh nganh, LopHanhChinh lop) {
        User user = taoUser(username, "123456", Role.SINH_VIEN);
        SinhVien sinhVien = new SinhVien();
        sinhVien.setUser(user);
        sinhVien.setMa(ma);
        sinhVien.setHoTen(hoTen);
        sinhVien.setEmail(email);
        sinhVien.setKhoa(khoa);
        sinhVien.setNganh(nganh);
        sinhVien.setLopHanhChinh(lop);
        return sinhVienRepository.save(sinhVien);
    }

    private LopHocPhan taoLopHocPhan(String ma, MonHoc monHoc, HocKy hocKy, GiangVien giangVien,
                                     String phong, int thu, int tietBatDau, int soTiet, int siSoToiDa) {
        LopHocPhan lop = new LopHocPhan();
        lop.setMa(ma);
        lop.setMonHoc(monHoc);
        lop.setHocKy(hocKy);
        lop.setGiangVien(giangVien);
        lop.setPhong(phong);
        lop.setThu(thu);
        lop.setTietBatDau(tietBatDau);
        lop.setSoTiet(soTiet);
        lop.setSiSoToiDa(siSoToiDa);
        lop.setSiSoHienTai(0);
        return lopHocPhanRepository.save(lop);
    }

    private void dangKyVoiDiem(SinhVien sinhVien, LopHocPhan lop, double quaTrinh, double cuoiKy) {
        DangKyHocPhan dangKy = dangKyHienTai(sinhVien, lop);
        Diem diem = new Diem();
        diem.setDangKy(dangKy);
        diem.setDiemQuaTrinh(quaTrinh);
        diem.setDiemCuoiKy(cuoiKy);
        diem.setDiemTongKet(Math.round((0.4 * quaTrinh + 0.6 * cuoiKy) * 10.0) / 10.0);
        diemRepository.save(diem);
    }

    private DangKyHocPhan dangKyHienTai(SinhVien sinhVien, LopHocPhan lop) {
        DangKyHocPhan dangKy = new DangKyHocPhan();
        dangKy.setSinhVien(sinhVien);
        dangKy.setLopHocPhan(lop);
        dangKy.setThoiGianDangKy(LocalDateTime.now());
        dangKy.setTrangThai(TrangThaiDangKy.DA_DANG_KY);
        lop.setSiSoHienTai(lop.getSiSoHienTai() + 1);
        return dangKyHocPhanRepository.save(dangKy);
    }
}
