package com.edu.registration.repository;

import com.edu.registration.entity.Nganh;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NganhRepository extends JpaRepository<Nganh, Long> {

    @EntityGraph(attributePaths = {"khoa"})
    List<Nganh> findAllByOrderByMaAsc();

    boolean existsByMa(String ma);

    Optional<Nganh> findByMa(String ma);
}
