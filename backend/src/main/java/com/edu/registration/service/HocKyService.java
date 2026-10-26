package com.edu.registration.service;

import com.edu.registration.dto.CatalogDtos;
import com.edu.registration.entity.HocKy;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.HocKyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class HocKyService {

    private final HocKyRepository hocKyRepository;
    private final DtoMapper mapper;

    public HocKyService(HocKyRepository hocKyRepository, DtoMapper mapper) {
        this.hocKyRepository = hocKyRepository;
        this.mapper = mapper;
    }

    public List<CatalogDtos.HocKyDto> danhSach() {
        return hocKyRepository.findAllByOrderByNgayBatDauDesc().stream().map(mapper::hocKy).toList();
    }

    public HocKy hocKyHienTai() {
        LocalDate homNay = LocalDate.now();
        return hocKyRepository
                .findFirstByDangKyTuLessThanEqualAndDangKyDenGreaterThanEqualOrderByDangKyDenDesc(homNay, homNay)
                .or(() -> hocKyRepository.findFirstByOrderByDangKyDenDesc())
                .orElse(null);
    }

    public HocKy lay(Long id) {
        return hocKyRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Học kỳ không tồn tại"));
    }

    public HocKy layHoacHienTai(Long id) {
        if (id != null) {
            return lay(id);
        }
        HocKy hocKy = hocKyHienTai();
        if (hocKy == null) {
            throw ApiException.notFound("Chưa có học kỳ nào trong hệ thống");
        }
        return hocKy;
    }
}
