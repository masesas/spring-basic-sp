package com.masesas.exercises.demo1.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "karyawan", schema = "masesas")
@NamedStoredProcedureQueries({

        // stored procedure dengan satu OUT parameter
        @NamedStoredProcedureQuery(
                name = "Karyawan.getTotalByStatus",
                procedureName = "masesas.sp_total_karyawan_by_status",
                parameters = {
                        @StoredProcedureParameter(mode = ParameterMode.IN, name = "status_in", type = String.class),
                        @StoredProcedureParameter(mode = ParameterMode.OUT, name = "total_out", type = Integer.class)
                }
        ),

        // stored procedure dengan beberapa OUT parameter
        @NamedStoredProcedureQuery(
                name = "Karyawan.getStatistikByStatus",
                procedureName = "masesas.sp_statistik_karyawan_by_status",
                parameters = {
                        @StoredProcedureParameter(mode = ParameterMode.IN, name = "status_in", type = String.class),
                        @StoredProcedureParameter(mode = ParameterMode.OUT, name = "total_out", type = Integer.class),
                        @StoredProcedureParameter(mode = ParameterMode.OUT, name = "umur_rata_out", type = BigDecimal.class),
                        @StoredProcedureParameter(mode = ParameterMode.OUT, name = "umur_minimum_out", type = Integer.class),
                        @StoredProcedureParameter(mode = ParameterMode.OUT, name = "umur_maksimum_out", type = Integer.class)
                }
        )
})
public class Karyawan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "nama")
    private String nama;

    @Column(name = "alamat", length = Integer.MAX_VALUE)
    private String alamat;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "status")
    private String status;

    @OneToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "detail_karyawan")
    private DetailKaryawan detailKaryawan;

    @Column(name = "created_date")
    private Instant createdDate;

    @Column(name = "deleted_date")
    private Instant deletedDate;

    @Column(name = "updated_date")
    private Instant updatedDate;


}