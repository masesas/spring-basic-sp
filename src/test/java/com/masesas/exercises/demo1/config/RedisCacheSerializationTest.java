package com.masesas.exercises.demo1.config;

import com.masesas.exercises.demo1.dto.DetailKaryawanResponse;
import com.masesas.exercises.demo1.dto.KaryawanResponse;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Memastikan KaryawanResponse bisa disimpan ke Redis sebagai JSON dan dibaca kembali utuh
 * (termasuk LocalDate dan Instant). Kalau ini gagal, cache akan error saat membaca data lama.
 */
class RedisCacheSerializationTest {

    private final RedisSerializer<Object> serializer = RedisSerializer.json();

    @Test
    @DisplayName("KaryawanResponse bolak-balik JSON tanpa kehilangan data")
    void karyawanResponse_serialisasiBolakBalik() {
        KaryawanResponse asli = new KaryawanResponse(
                1,
                "Budi",
                "Jakarta",
                LocalDate.of(1990, 1, 1),
                "AKTIF",
                null,
                new DetailKaryawanResponse(9, "3273010101900001", "09.254.294.3-407.000",
                        Instant.parse("2024-01-01T00:00:00Z"),
                        Instant.parse("2024-01-02T00:00:00Z")),
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-02T00:00:00Z"));

        byte[] json = serializer.serialize(asli);
        Object hasil = serializer.deserialize(json);

        assertThat(hasil).isInstanceOf(KaryawanResponse.class).isEqualTo(asli);
    }

    @Test
    @DisplayName("daftar karyawan (hasil get all) bolak-balik JSON tanpa kehilangan data")
    void daftarKaryawan_serialisasiBolakBalik() {
        List<KaryawanResponse> asli = new ArrayList<>(List.of(
                new KaryawanResponse(1, "Budi", "Jakarta", LocalDate.of(1990, 1, 1), "AKTIF", null, null,
                        Instant.parse("2024-01-01T00:00:00Z"), Instant.parse("2024-01-01T00:00:00Z")),
                new KaryawanResponse(2, "Siti", "Bandung", LocalDate.of(1995, 5, 5), "AKTIF", null, null,
                        Instant.parse("2024-02-01T00:00:00Z"), Instant.parse("2024-02-01T00:00:00Z"))));

        Object hasil = serializer.deserialize(serializer.serialize(asli));

        assertThat(hasil).asInstanceOf(InstanceOfAssertFactories.LIST).isEqualTo(asli);
    }

    @Test
    @DisplayName("daftar kosong tetap aman diserialisasi")
    void daftarKosong_serialisasiBolakBalik() {
        Object hasil = serializer.deserialize(serializer.serialize(new ArrayList<KaryawanResponse>()));

        assertThat(hasil).asInstanceOf(InstanceOfAssertFactories.LIST).isEmpty();
    }

    @Test
    @DisplayName("List.of tidak bisa dipakai sebagai nilai cache -- alasan findAll memakai ArrayList")
    void listImmutable_tidakBisaDibacaKembali() {
        List<KaryawanResponse> immutable = List.of(
                new KaryawanResponse(1, "Budi", "Jakarta", LocalDate.of(1990, 1, 1), "AKTIF", null, null,
                        Instant.parse("2024-01-01T00:00:00Z"), Instant.parse("2024-01-01T00:00:00Z")));

        byte[] json = serializer.serialize(immutable);

        // class immutable bersifat final -> Jackson tidak menulis info tipe -> gagal saat dibaca
        assertThatThrownBy(() -> serializer.deserialize(json))
                .isInstanceOf(SerializationException.class);
    }

    @Test
    @DisplayName("detail null tetap aman diserialisasi")
    void karyawanResponse_tanpaDetail() {
        KaryawanResponse asli = new KaryawanResponse(
                2, "Siti", null, LocalDate.of(1995, 5, 5), "AKTIF", null, null,
                Instant.parse("2024-02-01T00:00:00Z"),
                Instant.parse("2024-02-01T00:00:00Z"));

        Object hasil = serializer.deserialize(serializer.serialize(asli));

        assertThat(hasil).isEqualTo(asli);
    }
}
