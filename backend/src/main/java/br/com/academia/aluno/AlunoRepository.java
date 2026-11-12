package br.com.academia.aluno;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Todas as consultas daqui são filtradas pela academia do usuário logado (via {@code @TenantId} em
 * {@link Aluno}). Por isso "existsByCpf" significa "existe na MINHA academia".
 * <p>
 * A API identifica alunos pela matrícula ({@link #findByMatricula}); o id é só chave técnica interna.
 */
public interface AlunoRepository extends JpaRepository<Aluno, Long>, JpaSpecificationExecutor<Aluno> {

    Optional<Aluno> findByMatricula(String matricula);

    Optional<Aluno> findByRg(String rg);

    boolean existsByCpf(String cpf);

    boolean existsByCpfAndMatriculaNot(String cpf, String matricula);

    boolean existsByRg(String rg);

    boolean existsByRgAndMatriculaNot(String rg, String matricula);

    long countBySituacao(Situacao situacao);

    /**
     * Trava a linha do aluno até o fim da transação. Serializa registros de acesso concorrentes do
     * mesmo aluno (ex.: duplo toque na catraca) sem bloquear os demais.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Aluno a where a.matricula = :matricula")
    Optional<Aluno> findByMatriculaForUpdate(@Param("matricula") String matricula);
}
