package com.edu.registration.repository;

import com.edu.registration.entity.SinhVien;
import com.edu.registration.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SinhVienRepository extends JpaRepository<SinhVien, Long> {

    Optional<SinhVien> findByUser(User user);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sv from SinhVien sv where sv.user.id = :userId")
    Optional<SinhVien> findByUserIdForUpdate(@Param("userId") Long userId);

    Optional<SinhVien> findByMa(String ma);

    boolean existsByMa(String ma);

    @EntityGraph(attributePaths = {"user", "khoa", "nganh", "lopHanhChinh"})
    List<SinhVien> findAllByOrderByMaAsc();

    @EntityGraph(attributePaths = {"user", "khoa", "nganh", "lopHanhChinh"})
    List<SinhVien> findByHoTenContainingIgnoreCaseOrMaContainingIgnoreCaseOrderByMaAsc(String hoTen, String ma);
}
