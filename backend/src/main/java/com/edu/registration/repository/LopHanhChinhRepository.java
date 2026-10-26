package com.edu.registration.repository;

import com.edu.registration.entity.LopHanhChinh;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LopHanhChinhRepository extends JpaRepository<LopHanhChinh, Long> {

    @EntityGraph(attributePaths = {"nganh", "nganh.khoa"})
    List<LopHanhChinh> findAllByOrderByMaAsc();

    boolean existsByMa(String ma);

    Optional<LopHanhChinh> findByMa(String ma);
}
