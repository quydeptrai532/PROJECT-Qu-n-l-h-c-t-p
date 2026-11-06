package com.edu.registration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidationErrorDetail(
        Long dangKyId,
        Long sinhVienId,
        String maSinhVien,
        Integer dong,
        String cot,
        String field,
        String message
) {
    public ValidationErrorDetail(String cot, String message) {
        this(null, null, null, null, cot, cot, message);
    }
}
