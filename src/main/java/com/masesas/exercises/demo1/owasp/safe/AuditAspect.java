package com.masesas.exercises.demo1.owasp.safe;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Mencatat setiap operasi tulis pada slip gaji.
 *
 * <p>Ditempel di layer service, bukan controller. Kalau ditempel di controller, jalur
 * lain yang memanggil service langsung — job terjadwal, importer, controller baru —
 * tidak akan tercatat sama sekali.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogger auditLogger;

    @Pointcut("execution(* com.masesas.exercises.demo1.service.PayrollService.create(..)) "
            + "|| execution(* com.masesas.exercises.demo1.service.PayrollService.update(..)) "
            + "|| execution(* com.masesas.exercises.demo1.service.PayrollService.approve(..)) "
            + "|| execution(* com.masesas.exercises.demo1.service.PayrollService.delete(..))")
    void operasiTulisPayroll() {
    }

    @Around("operasiTulisPayroll()")
    public Object catatOperasi(ProceedingJoinPoint joinPoint) throws Throwable {
        String aksi = "payroll." + joinPoint.getSignature().getName();
        String sasaran = argumenRingkas(joinPoint);

        try {
            Object hasil = joinPoint.proceed();
            auditLogger.catat(aksi, sasaran, "BERHASIL");
            return hasil;
        } catch (Throwable ex) {
            auditLogger.catat(aksi, sasaran, "GAGAL:" + ex.getClass().getSimpleName());
            throw ex;
        }
    }

    /**
     * Hanya argumen bertipe sederhana yang ikut dicatat. Objek request lengkap bisa
     * memuat nominal gaji, dan jejak audit tidak boleh jadi tempat bocornya data yang
     * justru sedang kita lindungi.
     */
    private String argumenRingkas(ProceedingJoinPoint joinPoint) {
        return Arrays.stream(joinPoint.getArgs())
                .filter(arg -> arg instanceof Number || arg instanceof CharSequence
                        || arg instanceof java.time.temporal.Temporal)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
}
