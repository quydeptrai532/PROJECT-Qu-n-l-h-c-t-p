package com.edu.registration.repository;

import com.edu.registration.entity.MonTienQuyet;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Collection;

public interface MonTienQuyetRepository extends JpaRepository<MonTienQuyet, Long> {

    @EntityGraph(attributePaths = {"monHoc", "monTienQuyet"})
    List<MonTienQuyet> findByMonHocIdOrderByIdAsc(Long monHocId);

    @EntityGraph(attributePaths = {"monHoc", "monTienQuyet"})
    List<MonTienQuyet> findByMonHocIdInOrderByIdAsc(Collection<Long> monHocIds);

    boolean existsByMonHocIdAndMonTienQuyetId(Long monHocId, Long monTienQuyetId);

    boolean existsByMonHocId(Long monHocId);

    long countByMonTienQuyetId(Long monHocId);
}
