package com.edu.registration.repository;

import com.edu.registration.entity.Khoa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KhoaRepository extends JpaRepository<Khoa, Long> {

    List<Khoa> findAllByOrderByMaAsc();

    boolean existsByMa(String ma);

    Optional<Khoa> findByMa(String ma);
}
