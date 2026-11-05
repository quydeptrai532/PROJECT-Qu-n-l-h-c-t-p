package com.edu.registration.repository;

import com.edu.registration.entity.TepDinhKemYeuCau;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TepDinhKemYeuCauRepository extends JpaRepository<TepDinhKemYeuCau, Long> {

    List<TepDinhKemYeuCau> findByYeuCauIdOrderByIdAsc(Long yeuCauId);

    Optional<TepDinhKemYeuCau> findByIdAndYeuCauId(Long id, Long yeuCauId);

    long countByYeuCauId(Long yeuCauId);
}
