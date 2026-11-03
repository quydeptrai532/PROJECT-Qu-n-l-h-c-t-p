package com.edu.registration.repository;
import com.edu.registration.entity.KeHoachCaNhan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface KeHoachCaNhanRepository extends JpaRepository<KeHoachCaNhan,Long> {
 @EntityGraph(attributePaths={"mon","mon.lopHocPhan","mon.lopHocPhan.monHoc","mon.lopHocPhan.hocKy"}) List<KeHoachCaNhan> findBySinhVienIdOrderByCapNhatLucDesc(Long sinhVienId);
 @EntityGraph(attributePaths={"mon","mon.lopHocPhan","mon.lopHocPhan.monHoc","mon.lopHocPhan.hocKy"}) Optional<KeHoachCaNhan> findByIdAndSinhVienId(Long id,Long sinhVienId);
}
