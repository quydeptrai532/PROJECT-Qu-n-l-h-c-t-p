package com.edu.registration.repository;

import com.edu.registration.entity.LopHocPhan;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LopHocPhanRepository extends JpaRepository<LopHocPhan, Long> {

    @EntityGraph(attributePaths = {"monHoc", "monHoc.khoa", "hocKy", "giangVien"})
    List<LopHocPhan> findByHocKyIdOrderByMaAsc(Long hocKyId);

    @EntityGraph(attributePaths = {"monHoc", "monHoc.khoa", "hocKy", "giangVien"})
    List<LopHocPhan> findByGiangVienIdAndHocKyIdOrderByMaAsc(Long giangVienId, Long hocKyId);

    @EntityGraph(attributePaths = {"monHoc", "monHoc.khoa", "hocKy", "giangVien"})
    List<LopHocPhan> findByGiangVienIdOrderByMaAsc(Long giangVienId);

    @EntityGraph(attributePaths = {"monHoc", "monHoc.khoa", "hocKy", "giangVien"})
    @Query("select l from LopHocPhan l where l.id = :id")
    Optional<LopHocPhan> findWithDetailsById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from LopHocPhan l where l.id = :id")
    Optional<LopHocPhan> findByIdForUpdate(@Param("id") Long id);

    boolean existsByMa(String ma);

    long countByHocKyId(Long hocKyId);
}
