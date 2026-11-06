package com.edu.registration.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Entity @Table(name="ke_hoach_ca_nhan")
public class KeHoachCaNhan {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="sinh_vien_id",nullable=false) private SinhVien sinhVien;
 @Column(nullable=false,length=120) private String ten;
 @Column(nullable=false) private LocalDateTime capNhatLuc=LocalDateTime.now();
 @OneToMany(mappedBy="keHoach",cascade=CascadeType.ALL,orphanRemoval=true) private List<KeHoachMon> mon=new ArrayList<>();
 public Long getId(){return id;} public SinhVien getSinhVien(){return sinhVien;} public void setSinhVien(SinhVien v){sinhVien=v;} public String getTen(){return ten;} public void setTen(String v){ten=v;} public LocalDateTime getCapNhatLuc(){return capNhatLuc;} public void setCapNhatLuc(LocalDateTime v){capNhatLuc=v;} public List<KeHoachMon> getMon(){return mon;}
}
