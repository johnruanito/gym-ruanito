package br.com.academia.aluno;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Critérios opcionais de pesquisa. Substitui a montagem de JPQL por concatenação de strings
 * ("... AND 1 = 1") do sistema antigo por Criteria API tipada.
 */
public record AlunoFiltro(String matricula, String nome, String rg, String cpf, String celular, Situacao situacao) {

    Specification<Aluno> toSpecification() {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (!isBlank(matricula)) {
                p.add(cb.equal(root.get("matricula"), matricula.strip()));
            }
            if (!isBlank(nome)) {
                // Busca case-insensitive; acelerada por índice trigram (pg_trgm) em lower(nome).
                p.add(cb.like(cb.lower(root.get("nome")), "%" + escapeLike(nome.strip().toLowerCase(Locale.ROOT)) + "%", '\\'));
            }
            if (!isBlank(rg)) {
                p.add(cb.equal(root.get("rg"), rg.strip()));
            }
            if (!isBlank(cpf)) {
                p.add(cb.equal(root.get("cpf"), Cpf.normalizar(cpf)));
            }
            if (!isBlank(celular)) {
                String digitos = celular.replaceAll("\\D", "");
                p.add(cb.like(root.get("celular").get("numero"), "%" + digitos + "%"));
            }
            if (situacao != null) {
                p.add(cb.equal(root.get("situacao"), situacao));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
