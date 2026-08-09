package com.masesas.exercises.demo1.service;

import com.masesas.exercises.demo1.dto.Karyawan2Request;
import com.masesas.exercises.demo1.dto.Karyawan2Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class Karyawan2Service {

    private static final String BASE_SQL = """
            SELECT 
                id, 
                nama, 
                alamat, 
                dob, 
                status 
            FROM 
                masesas.karyawan 
            WHERE deleted_date IS NULL
            """;

    private static final String COUNT_SQL =
            "SELECT count(*) FROM masesas.karyawan WHERE deleted_date IS NULL";

    private final JdbcTemplate jdbcTemplate;


    public List<Karyawan2Response> getAll() {
        return jdbcTemplate.query(BASE_SQL + " ORDER BY id", rowMapper());
    }

    public Karyawan2Response getById(Integer id) {
        List<Karyawan2Response> result = jdbcTemplate.query(BASE_SQL + " AND id = ?", rowMapper(), id);
        return result.isEmpty() ? null : result.get(0);
    }

    public List<Karyawan2Response> getAllByStatus(String status) {
        return jdbcTemplate.query(BASE_SQL + " AND status = ? ORDER BY id", rowMapper(), status);
    }

    public Page<Karyawan2Response> getPage(int page, int size) {
        try {
            List<Karyawan2Response> content = jdbcTemplate.query(
                    BASE_SQL + " ORDER BY id LIMIT ? OFFSET ?",
                    rowMapper(),
                    size,
                    page * size);
            Long total = jdbcTemplate.queryForObject(COUNT_SQL, Long.class);
            return new PageImpl<>(content, PageRequest.of(page, size), total == null ? 0 : total);
        } catch (DataAccessException e) {
            // e ditaruh sebagai argumen terakhir tanpa placeholder supaya SLF4J mencetak
            // stacktrace-nya; e.getMessage() saja membuang jejak asal masalahnya.
            log.error("Gagal mengambil halaman karyawan page={} size={}", page, size, e);
            throw e;
        }
    }

    public Page<Karyawan2Response> getPageByNama(String nama, int page, int size) {
        String keyword = "%" + nama + "%";
        List<Karyawan2Response> content = jdbcTemplate.query(
                BASE_SQL + " AND nama ILIKE ? ORDER BY id LIMIT ? OFFSET ?",
                rowMapper(),
                keyword,
                size,
                page * size);
        Long total = jdbcTemplate.queryForObject(COUNT_SQL + " AND nama ILIKE ?", Long.class, keyword);
        return new PageImpl<>(content, PageRequest.of(page, size), total == null ? 0 : total);
    }

    public Karyawan2Response insert(Karyawan2Request request) {
        Integer id = jdbcTemplate.queryForObject(
                "INSERT INTO masesas.karyawan (nama, alamat, dob, status, created_date) "
                        + "VALUES (?, ?, ?, ?, now()) RETURNING id",
                Integer.class,
                request.nama(),
                request.alamat(),
                request.dob(),
                request.status());
        return getById(id);
    }

    public int insertBatch(List<Karyawan2Request> requests) {
        List<Object[]> params = new ArrayList<>();
        for (Karyawan2Request request : requests) {
            params.add(new Object[]{request.nama(), request.alamat(), request.dob(), request.status()});
        }
        int[] affected = jdbcTemplate.batchUpdate(
                "INSERT INTO masesas.karyawan (nama, alamat, dob, status, created_date) "
                        + "VALUES (?, ?, ?, ?, now())",
                params);
        return affected.length;
    }

    public Karyawan2Response update(Integer id, Karyawan2Request request) {
        int affected = jdbcTemplate.update(
                "UPDATE masesas.karyawan SET nama = ?, alamat = ?, dob = ?, status = ?, updated_date = now() "
                        + "WHERE id = ? AND deleted_date IS NULL",
                request.nama(),
                request.alamat(),
                request.dob(),
                request.status(),
                id);
        return affected == 0 ? null : getById(id);
    }

    public boolean delete(Integer id) {
        int affected = jdbcTemplate.update(
                "UPDATE masesas.karyawan SET deleted_date = now() WHERE id = ? AND deleted_date IS NULL",
                id);
        return affected > 0;
    }

    private RowMapper<Karyawan2Response> rowMapper() {
        return (ResultSet rs, int rowNum) -> {
            Karyawan2Response response = new Karyawan2Response();
            response.setId(rs.getInt("id"));
            response.setNama(rs.getString("alamat"));
            return response;
        };
    }
}
