package com.edu.registration.repository;

import com.edu.registration.entity.SinhVien;
import com.edu.registration.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SinhVienRepository extends JpaRepository<SinhVien, Long> {

    Optional<SinhVien> findByUser(User user);

    Optional<SinhVien> findByMa(String ma);

    boolean existsByMa(String ma);

    @EntityGraph(attributePaths = {"user", "khoa", "nganh", "lopHanhChinh"})
    List<SinhVien> findAllByOrderByMaAsc();

    @EntityGraph(attributePaths = {"user", "khoa", "nganh", "lopHanhChinh"})
    List<SinhVien> findByHoTenContainingIgnoreCaseOrMaContainingIgnoreCaseOrderByMaAsc(String hoTen, String ma);
}
