package com.edu.registration.repository;

import com.edu.registration.entity.ThongBaoLopHocPhan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ThongBaoLopHocPhanRepository extends JpaRepository<ThongBaoLopHocPhan, Long> {

    @Query("SELECT tb FROM ThongBaoLopHocPhan tb JOIN FETCH tb.lopHocPhan lop JOIN FETCH lop.monHoc JOIN FETCH tb.giangVien WHERE lop.id = :lopHocPhanId ORDER BY tb.thoiGianTao DESC, tb.id DESC")
    List<ThongBaoLopHocPhan> findByLopHocPhanIdOrderByThoiGianTaoDesc(@Param("lopHocPhanId") Long lopHocPhanId);

    @Query("SELECT tb FROM ThongBaoLopHocPhan tb JOIN FETCH tb.lopHocPhan lop JOIN FETCH lop.monHoc JOIN FETCH tb.giangVien WHERE tb.giangVien.id = :giangVienId ORDER BY tb.thoiGianTao DESC, tb.id DESC")
    List<ThongBaoLopHocPhan> findByGiangVienIdOrderByThoiGianTaoDesc(@Param("giangVienId") Long giangVienId);

    Optional<ThongBaoLopHocPhan> findByLopHocPhanIdAndIdempotencyKey(Long lopHocPhanId, String idempotencyKey);
}
