package br.com.academia.acesso;

import br.com.academia.aluno.Aluno;
import br.com.academia.aluno.AlunoRepository;
import br.com.academia.shared.config.AcademiaProperties;
import br.com.academia.shared.error.RecursoNaoEncontradoException;
import br.com.academia.shared.error.RegraNegocioException;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AcessoService {

    /** Resultado de um registro na catraca/recepção. */
    public record Registro(TipoAcesso tipo, String matricula, String nome, Instant horario) {
    }

    private final AcessoRepository acessos;
    private final AlunoRepository alunos;
    private final Clock clock;
    private final ZoneId zone;
    private final Duration permanenciaMaxima;
    private final MeterRegistry metrics;

    AcessoService(AcessoRepository acessos, AlunoRepository alunos, Clock clock, AcademiaProperties props,
                  MeterRegistry metrics) {
        this.acessos = acessos;
        this.alunos = alunos;
        this.clock = clock;
        this.zone = props.zone();
        this.permanenciaMaxima = props.acesso().permanenciaMaxima();
        this.metrics = metrics;
    }

    /**
     * Alterna entrada/saída do aluno identificado por matrícula ou RG.
     * <ul>
     *   <li>Há entrada aberta e recente → registra a saída (permitida mesmo para aluno inativo).</li>
     *   <li>Caso contrário → registra nova entrada, somente se o aluno estiver ATIVO.</li>
     * </ul>
     */
    @Transactional
    public Registro registrar(String matricula, String rg) {
        Aluno aluno = localizarComLock(matricula, rg);
        Instant agora = clock.instant();

        var ultimo = acessos.findFirstByAlunoOrderByEntradaDesc(aluno);
        if (ultimo.isPresent() && ultimo.get().aberto() && !ultimo.get().abandonado(agora, permanenciaMaxima)) {
            ultimo.get().fechar(agora);
            contar("SAIDA");
            return new Registro(TipoAcesso.SAIDA, aluno.getMatricula(), aluno.getNome(), agora);
        }

        if (!aluno.getSituacao().permiteEntrada()) {
            contar("BLOQUEADA");
            throw new RegraNegocioException("Entrada bloqueada: aluno com situação " + aluno.getSituacao());
        }
        acessos.save(Acesso.abrir(aluno, agora));
        contar("ENTRADA");
        return new Registro(TipoAcesso.ENTRADA, aluno.getMatricula(), aluno.getNome(), agora);
    }

    /**
     * Relatório de entradas/saídas. O filtro é sempre pela data de ENTRADA
     * (o sistema antigo filtrava a data final pela saída e omitia quem ainda estava na academia).
     */
    public Page<Acesso> relatorio(String matricula, LocalDate de, LocalDate ate, Pageable pageable) {
        LocalDate hoje = LocalDate.now(clock);
        LocalDate fim = ate != null ? ate : hoje;
        LocalDate inicio = de != null ? de : fim.minusDays(30);
        if (inicio.isAfter(fim)) {
            throw new RegraNegocioException("A data inicial deve ser anterior ou igual à data final");
        }
        Instant ini = inicio.atStartOfDay(zone).toInstant();
        Instant fimExclusivo = fim.plusDays(1).atStartOfDay(zone).toInstant();

        return matricula == null || matricula.isBlank()
                ? acessos.findPeriodo(ini, fimExclusivo, pageable)
                : acessos.findPeriodoPorAluno(matricula.strip(), ini, fimExclusivo, pageable);
    }

    /** Alunos que entraram e ainda não saíram (desconsiderando entradas abandonadas). */
    public List<Acesso> presentes() {
        return acessos.findBySaidaIsNullAndEntradaAfterOrderByEntradaDesc(clock.instant().minus(permanenciaMaxima));
    }

    public long totalPresentes() {
        return acessos.countBySaidaIsNullAndEntradaAfter(clock.instant().minus(permanenciaMaxima));
    }

    public long entradasHoje() {
        return acessos.countByEntradaGreaterThanEqual(LocalDate.now(clock).atStartOfDay(zone).toInstant());
    }

    /** Métrica de negócio exposta no Prometheus como academia_acessos_total{resultado=...}. */
    private void contar(String resultado) {
        metrics.counter("academia.acessos", "resultado", resultado).increment();
    }

    private Aluno localizarComLock(String matricula, String rg) {
        boolean semMatricula = matricula == null || matricula.isBlank();
        boolean semRg = rg == null || rg.isBlank();
        if (semMatricula && semRg) {
            throw new RegraNegocioException("Informe a matrícula ou o RG do aluno");
        }
        String chave = semMatricula
                ? alunos.findByRg(rg.strip()).map(Aluno::getMatricula).orElse(null)
                : matricula.strip();
        if (chave == null) {
            throw new RecursoNaoEncontradoException("Aluno não encontrado");
        }
        return alunos.findByMatriculaForUpdate(chave)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado"));
    }
}
