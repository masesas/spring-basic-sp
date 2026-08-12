package com.masesas.exercises.demo1.service;

import com.masesas.exercises.demo1.config.SecurityConfig;
import com.masesas.exercises.demo1.dto.EndpointAksesResponse;
import com.masesas.exercises.demo1.dto.RoleAksesResponse;
import com.masesas.exercises.demo1.entity.Role;
import com.masesas.exercises.demo1.repository.RoleRepository;
import com.masesas.exercises.demo1.security.AppUser;
import com.masesas.exercises.demo1.security.AppUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class RoleMapService {

    private static final String PERAN_GUEST = "GUEST";
    private static final String METODE_SEMUA = "ANY";
    private static final Pattern PEMANGGILAN_PERAN = Pattern.compile("has(?:Any)?Role\\(([^)]*)\\)");
    private static final Pattern NAMA_PERAN = Pattern.compile("'([^']*)'");
    private static final Pattern PENGHUBUNG = Pattern.compile("[\\s()]|\\band\\b|\\bor\\b");

    private final AntPathMatcher pencocokPath = new AntPathMatcher();

    @Qualifier("requestMappingHandlerMapping")
    private final RequestMappingHandlerMapping handlerMapping;

    private final RoleRepository roleRepository;

    public List<EndpointAksesResponse> semua() {
        return handlerMapping.getHandlerMethods().entrySet().stream()
                .flatMap(entri -> uraikan(entri.getKey(), entri.getValue()))
                .sorted(Comparator.comparing(EndpointAksesResponse::getPath)
                        .thenComparing(EndpointAksesResponse::getMethod))
                .toList();
    }

    public RoleAksesResponse untukPeran(String peran) {
        return rangkum(peran.toUpperCase(Locale.ROOT), semua());
    }

    public List<RoleAksesResponse> semuaPeran() {
        List<EndpointAksesResponse> endpoint = semua();
        return daftarPeran(endpoint).stream()
                .map(peran -> rangkum(peran, endpoint))
                .toList();
    }

    private RoleAksesResponse rangkum(String peran, List<EndpointAksesResponse> endpoint) {
        List<EndpointAksesResponse> hasil = endpoint.stream()
                .filter(satu -> bisaDiakses(satu, peran))
                .toList();
        return new RoleAksesResponse(peran, hasil.size(), hasil);
    }

    private Set<String> daftarPeran(List<EndpointAksesResponse> endpoint) {
        Set<String> peran = new TreeSet<>();
        roleRepository.findAll().stream()
                .map(Role::getNama)
                .filter(Objects::nonNull)
                .forEach(nama -> peran.add(nama.toUpperCase(Locale.ROOT)));
        endpoint.forEach(satu -> peran.addAll(satu.getRoles()));
        peran.add(AppUserDetailsService.ROLE_CUSTOMER);
        peran.add(AppUser.ROLE_SUPERADMIN);
        peran.add(PERAN_GUEST);
        return peran;
    }

    private Stream<EndpointAksesResponse> uraikan(RequestMappingInfo info, HandlerMethod handler) {
        String ekspresi = ekspresiOtorisasi(handler);
        List<String> peran = peranDari(ekspresi);
        boolean kondisional = ekspresi != null && kondisional(ekspresi);
        String namaHandler = handler.getBeanType().getSimpleName() + "." + handler.getMethod().getName();

        List<String> metode = info.getMethodsCondition().getMethods().isEmpty()
                ? List.of(METODE_SEMUA)
                : info.getMethodsCondition().getMethods().stream().map(Enum::name).sorted().toList();

        Set<String> path = info.getPathPatternsCondition() == null
                ? info.getDirectPaths()
                : info.getPathPatternsCondition().getPatternValues();

        return path.stream().flatMap(satuPath -> metode.stream().map(satuMetode ->
                new EndpointAksesResponse(
                        satuMetode, satuPath, namaHandler, publik(satuPath), peran, kondisional, ekspresi)));
    }

    private String ekspresiOtorisasi(HandlerMethod handler) {
        PreAuthorize padaMethod =
                AnnotatedElementUtils.findMergedAnnotation(handler.getMethod(), PreAuthorize.class);
        if (padaMethod != null) {
            return padaMethod.value();
        }
        PreAuthorize padaClass =
                AnnotatedElementUtils.findMergedAnnotation(handler.getBeanType(), PreAuthorize.class);
        return padaClass == null ? null : padaClass.value();
    }

    private List<String> peranDari(String ekspresi) {
        if (ekspresi == null) {
            return List.of();
        }
        List<String> peran = new ArrayList<>();
        Matcher pemanggilan = PEMANGGILAN_PERAN.matcher(ekspresi);
        while (pemanggilan.find()) {
            Matcher nama = NAMA_PERAN.matcher(pemanggilan.group(1));
            while (nama.find()) {
                peran.add(nama.group(1));
            }
        }
        return List.copyOf(peran);
    }

    private boolean kondisional(String ekspresi) {
        String tanpaPeran = PEMANGGILAN_PERAN.matcher(ekspresi).replaceAll("");
        return !PENGHUBUNG.matcher(tanpaPeran).replaceAll("").isEmpty();
    }

    private boolean publik(String path) {
        return SecurityConfig.WHITELIST_ENDPOINTS.stream().anyMatch(pola -> pencocokPath.match(pola, path));
    }

    private boolean bisaDiakses(EndpointAksesResponse endpoint, String peran) {
        if (endpoint.isPublic()) {
            return true;
        }
        if (PERAN_GUEST.equals(peran)) {
            return false;
        }
        if (AppUser.ROLE_SUPERADMIN.equals(peran)) {
            return true;
        }
        if (endpoint.getExpressions() == null) {
            return true;
        }
        return endpoint.getRoles().contains(peran) || endpoint.isConditional();
    }
}
