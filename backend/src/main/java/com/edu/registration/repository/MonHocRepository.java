package com.edu.registration.repository;

import com.edu.registration.entity.MonHoc;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MonHocRepository extends JpaRepository<MonHoc, Long> {

    @EntityGraph(attributePaths = {"khoa"})
    List<MonHoc> findAllByOrderByMaAsc();

    boolean existsByMa(String ma);

    Optional<MonHoc> findByMa(String ma);
}
