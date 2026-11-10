package br.com.academia.acesso;

import br.com.academia.aluno.Aluno;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AcessoRepository extends JpaRepository<Acesso, Long> {

    Optional<Acesso> findFirstByAlunoOrderByEntradaDesc(Aluno aluno);

    /** Período por data de entrada; aluno carregado no mesmo SELECT (sem N+1). */
    @Query(value = """
            select a from Acesso a join fetch a.aluno
            where a.entrada >= :inicio and a.entrada < :fim
            order by a.entrada desc
            """,
            countQuery = "select count(a) from Acesso a where a.entrada >= :inicio and a.entrada < :fim")
    Page<Acesso> findPeriodo(@Param("inicio") Instant inicio, @Param("fim") Instant fim, Pageable pageable);

    @Query(value = """
            select a from Acesso a join fetch a.aluno al
            where al.matricula = :matricula and a.entrada >= :inicio and a.entrada < :fim
            order by a.entrada desc
            """,
            countQuery = """
                    select count(a) from Acesso a
                    where a.aluno.matricula = :matricula and a.entrada >= :inicio and a.entrada < :fim
                    """)
    Page<Acesso> findPeriodoPorAluno(@Param("matricula") String matricula,
                                     @Param("inicio") Instant inicio,
                                     @Param("fim") Instant fim,
                                     Pageable pageable);

    @EntityGraph(attributePaths = "aluno")
    List<Acesso> findBySaidaIsNullAndEntradaAfterOrderByEntradaDesc(Instant limite);

    long countBySaidaIsNullAndEntradaAfter(Instant limite);

    long countByEntradaGreaterThanEqual(Instant inicio);
}
