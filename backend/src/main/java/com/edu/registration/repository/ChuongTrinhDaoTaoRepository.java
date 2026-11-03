package com.edu.registration.repository;
import com.edu.registration.entity.ChuongTrinhDaoTao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ChuongTrinhDaoTaoRepository extends JpaRepository<ChuongTrinhDaoTao,Long> {
 @EntityGraph(attributePaths={"nganh","nhom","nhom.monHoc","nhom.monHoc.khoa"}) Optional<ChuongTrinhDaoTao> findWithDetailsById(Long id);
 @EntityGraph(attributePaths={"nganh","nhom","nhom.monHoc"}) List<ChuongTrinhDaoTao> findAll();
}
