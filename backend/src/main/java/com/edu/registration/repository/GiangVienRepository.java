package com.edu.registration.repository;

import com.edu.registration.entity.GiangVien;
import com.edu.registration.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GiangVienRepository extends JpaRepository<GiangVien, Long> {

    Optional<GiangVien> findByUser(User user);

    Optional<GiangVien> findByMa(String ma);

    boolean existsByMa(String ma);

    @EntityGraph(attributePaths = {"user", "khoa"})
    List<GiangVien> findAllByOrderByMaAsc();

    @EntityGraph(attributePaths = {"user", "khoa"})
    List<GiangVien> findByHoTenContainingIgnoreCaseOrMaContainingIgnoreCaseOrderByMaAsc(String hoTen, String ma);
}
