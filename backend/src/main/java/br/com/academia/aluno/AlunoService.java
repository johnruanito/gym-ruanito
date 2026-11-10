package br.com.academia.aluno;

import br.com.academia.estado.EstadoRepository;
import br.com.academia.shared.error.ConflitoException;
import br.com.academia.shared.error.RecursoNaoEncontradoException;
import br.com.academia.shared.error.RegraNegocioException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class AlunoService {

    private final AlunoRepository alunos;
    private final EstadoRepository estados;
    private final MatriculaGenerator matriculas;

    AlunoService(AlunoRepository alunos, EstadoRepository estados, MatriculaGenerator matriculas) {
        this.alunos = alunos;
        this.estados = estados;
        this.matriculas = matriculas;
    }

    public Aluno buscar(String matricula) {
        return alunos.findById(matricula)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno " + matricula + " não encontrado"));
    }

    public Page<Aluno> pesquisar(AlunoFiltro filtro, Pageable pageable) {
        return alunos.findAll(filtro.toSpecification(), pageable);
    }

    @Transactional
    public Aluno cadastrar(DadosAluno dados) {
        validar(dados, null);
        Aluno aluno = new Aluno(matriculas.proxima(), dados);
        return alunos.save(aluno);
    }

    @Transactional
    public Aluno atualizar(String matricula, DadosAluno dados, Long versaoEsperada) {
        Aluno aluno = buscar(matricula);
        if (versaoEsperada != null && versaoEsperada != aluno.getVersao()) {
            throw new ConflitoException("O cadastro foi alterado por outro usuário. Recarregue e tente novamente.");
        }
        validar(dados, matricula);
        aluno.atualizar(dados);
        return aluno;
    }

    /**
     * Exclui um aluno sem histórico. Com histórico de acessos a exclusão é bloqueada (FK RESTRICT):
     * o correto é inativar, preservando os relatórios. No sistema antigo isso estourava erro de FK na tela.
     */
    @Transactional
    public void excluir(String matricula) {
        Aluno aluno = buscar(matricula);
        try {
            alunos.delete(aluno);
            alunos.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflitoException(
                    "O aluno possui histórico de acessos e não pode ser excluído. Altere a situação para INATIVO.");
        }
    }

    public Map<Situacao, Long> contarPorSituacao() {
        Map<Situacao, Long> totais = new EnumMap<>(Situacao.class);
        for (Situacao s : Situacao.values()) {
            totais.put(s, alunos.countBySituacao(s));
        }
        return totais;
    }

    private void validar(DadosAluno dados, String matriculaAtual) {
        String cpf = Cpf.normalizar(dados.cpf());
        if (!Cpf.isValido(cpf)) {
            throw new RegraNegocioException("CPF inválido");
        }
        if (!estados.existsById(dados.endereco().estado())) {
            throw new RegraNegocioException("Estado (UF) inválido: " + dados.endereco().estado());
        }
        boolean cpfDuplicado = matriculaAtual == null
                ? alunos.existsByCpf(cpf)
                : alunos.existsByCpfAndMatriculaNot(cpf, matriculaAtual);
        if (cpfDuplicado) {
            throw new ConflitoException("Já existe aluno cadastrado com este CPF");
        }
        boolean rgDuplicado = matriculaAtual == null
                ? alunos.existsByRg(dados.rg())
                : alunos.existsByRgAndMatriculaNot(dados.rg(), matriculaAtual);
        if (rgDuplicado) {
            throw new ConflitoException("Já existe aluno cadastrado com este RG");
        }
    }
}
