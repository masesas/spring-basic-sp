package com.masesas.exercises.demo1.owasp.safe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class AuditLogger {

    /**
     * Logger bernama AUDIT, bukan nama kelas. Jejak audit punya pembaca dan masa simpan
     * yang berbeda dari log debug biasa, jadi harus bisa dipisahkan ke appender sendiri
     * tanpa menyeret seluruh log aplikasi.
     */
    private static final Logger log = LoggerFactory.getLogger("AUDIT");

    private static final String ANONIM = "anonim";

    public void catat(String aksi, String sasaran, String hasil) {
        log.info("aksi={} sasaran={} aktor={} peran={} ip={} hasil={}",
                aksi,
                InputSanitizer.untukLog(sasaran),
                aktor(),
                peran(),
                ipPemanggil(),
                hasil);
    }

    private String aktor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? ANONIM : InputSanitizer.untukLog(authentication.getName());
    }

    private String peran() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return "-";
        }
        return authentication.getAuthorities().stream()
                .map(Object::toString)
                .reduce((a, b) -> a + "," + b)
                .orElse("-");
    }

    private String ipPemanggil() {
        if (RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest().getRemoteAddr();
        }
        return "-";
    }
}
