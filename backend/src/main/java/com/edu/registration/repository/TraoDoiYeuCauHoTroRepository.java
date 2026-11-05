package com.edu.registration.repository;

import com.edu.registration.entity.TraoDoiYeuCauHoTro;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TraoDoiYeuCauHoTroRepository extends JpaRepository<TraoDoiYeuCauHoTro, Long> {

    @EntityGraph(attributePaths = {"nguoiGui"})
    List<TraoDoiYeuCauHoTro> findByYeuCauIdOrderByCreatedAtAscIdAsc(Long yeuCauId);

    long countByYeuCauId(Long yeuCauId);
}
