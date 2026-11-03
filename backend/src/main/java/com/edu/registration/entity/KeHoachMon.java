package com.edu.registration.entity;
import jakarta.persistence.*;
@Entity @Table(name="ke_hoach_mon",uniqueConstraints=@UniqueConstraint(columnNames={"ke_hoach_id","lop_hoc_phan_id"}))
public class KeHoachMon {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="ke_hoach_id",nullable=false) private KeHoachCaNhan keHoach;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="lop_hoc_phan_id",nullable=false) private LopHocPhan lopHocPhan;
 public Long getId(){return id;} public KeHoachCaNhan getKeHoach(){return keHoach;} public void setKeHoach(KeHoachCaNhan v){keHoach=v;} public LopHocPhan getLopHocPhan(){return lopHocPhan;} public void setLopHocPhan(LopHocPhan v){lopHocPhan=v;}
}
