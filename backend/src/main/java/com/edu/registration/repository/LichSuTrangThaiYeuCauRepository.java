package com.edu.registration.repository;

import com.edu.registration.entity.LichSuTrangThaiYeuCau;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LichSuTrangThaiYeuCauRepository extends JpaRepository<LichSuTrangThaiYeuCau, Long> {

    @EntityGraph(attributePaths = {"nguoiThucHien"})
    List<LichSuTrangThaiYeuCau> findByYeuCauIdOrderByThoiDiemAscIdAsc(Long yeuCauId);
}
