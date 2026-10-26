package com.edu.registration.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "mon_tien_quyet", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"mon_hoc_id", "mon_tien_quyet_id"})
})
public class MonTienQuyet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mon_hoc_id", nullable = false)
    private MonHoc monHoc;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mon_tien_quyet_id", nullable = false)
    private MonHoc monTienQuyet;

    public MonTienQuyet() {
    }

    public MonTienQuyet(MonHoc monHoc, MonHoc monTienQuyet) {
        this.monHoc = monHoc;
        this.monTienQuyet = monTienQuyet;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MonHoc getMonHoc() {
        return monHoc;
    }

    public void setMonHoc(MonHoc monHoc) {
        this.monHoc = monHoc;
    }

    public MonHoc getMonTienQuyet() {
        return monTienQuyet;
    }

    public void setMonTienQuyet(MonHoc monTienQuyet) {
        this.monTienQuyet = monTienQuyet;
    }
}
