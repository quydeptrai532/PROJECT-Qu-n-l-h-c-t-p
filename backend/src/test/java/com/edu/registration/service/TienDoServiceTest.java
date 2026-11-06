package com.edu.registration.service;

import com.edu.registration.entity.*;
import com.edu.registration.repository.*;
import com.edu.registration.security.CurrentUser;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TienDoServiceTest {
 @Test void noAssignedCurriculumIsExplicitlyUnconfigured(){
  SinhVienRepository students=mock(SinhVienRepository.class);CurrentUser current=mock(CurrentUser.class);DangKyHocPhanRepository regs=mock(DangKyHocPhanRepository.class);DiemRepository grades=mock(DiemRepository.class);ChuongTrinhDaoTaoRepository programs=mock(ChuongTrinhDaoTaoRepository.class);
  User u=new User();SinhVien sv=new SinhVien();sv.setUser(u);when(current.get()).thenReturn(u);when(students.findByUser(u)).thenReturn(Optional.of(sv));
  Map<String,Object> result=new TienDoService(students,current,regs,grades,programs).get();
  assertEquals(false,result.get("configured"));assertTrue(((String)result.get("message")).contains("Chưa được gán"));verifyNoInteractions(regs,grades,programs);
 }

 @Test void passedSubjectCreditsAreCountedOnlyOnceAndProgressIsAccountScoped(){
  SinhVienRepository students=mock(SinhVienRepository.class);CurrentUser current=mock(CurrentUser.class);DangKyHocPhanRepository regs=mock(DangKyHocPhanRepository.class);DiemRepository grades=mock(DiemRepository.class);ChuongTrinhDaoTaoRepository programs=mock(ChuongTrinhDaoTaoRepository.class);
  User u=new User();SinhVien sv=new SinhVien();sv.setId(7L);sv.setUser(u);when(current.get()).thenReturn(u);when(students.findByUser(u)).thenReturn(Optional.of(sv));
  ChuongTrinhDaoTao program=new ChuongTrinhDaoTao();program.setId(12L);Nganh major=new Nganh();major.setTen("Kỹ thuật");program.setNganh(major);program.setKhoaNhapHoc("2025");program.setPhienBan("v1");when(programs.findWithDetailsById(12L)).thenReturn(Optional.of(program));sv.setChuongTrinh(program);
  MonHoc subject=new MonHoc();subject.setId(3L);subject.setMa("CS101");subject.setTen("Nhập môn");subject.setSoTinChi(3);
  NhomChuongTrinh group=new NhomChuongTrinh();group.setTen("Bắt buộc");group.setLoai(NhomChuongTrinh.LoaiNhom.BAT_BUOC);group.setTinChiYeuCau(3);group.getMonHoc().add(subject);group.getMonHoc().add(subject);program.getNhom().add(group);
  HocKy term=new HocKy();term.setNgayKetThuc(java.time.LocalDate.now().plusDays(10));LopHocPhan section=new LopHocPhan();section.setMonHoc(subject);section.setHocKy(term);DangKyHocPhan enrollment=new DangKyHocPhan();enrollment.setId(9L);enrollment.setLopHocPhan(section);when(regs.findAllActiveOfStudent(7L,TrangThaiDangKy.DA_DANG_KY)).thenReturn(List.of(enrollment));
  Diem grade=new Diem();grade.setDangKy(enrollment);grade.setDiemTongKet(8.5);when(grades.findByDangKyIdIn(List.of(9L))).thenReturn(List.of(grade));
  Map<String,Object> result=new TienDoService(students,current,regs,grades,programs).get();
  assertEquals(3,result.get("requiredCredits"));assertEquals(3,result.get("earnedCredits"));assertEquals(100,result.get("completionPercent"));
  List<?> groups=(List<?>)result.get("groups");Map<?,?> rendered=(Map<?,?>)groups.getFirst();assertEquals(1,((List<?>)rendered.get("courses")).size());assertEquals("PASSED",((Map<?,?>)((List<?>)rendered.get("courses")).getFirst()).get("status"));
  verify(regs).findAllActiveOfStudent(7L,TrangThaiDangKy.DA_DANG_KY);
 }
}
