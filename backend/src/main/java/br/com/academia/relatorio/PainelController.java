package br.com.academia.relatorio;

import br.com.academia.acesso.AcessoService;
import br.com.academia.aluno.AlunoService;
import br.com.academia.aluno.Situacao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Indicadores da tela inicial. O relatório por situação do sistema antigo virou o filtro
 * {@code GET /api/alunos?situacao=...}; aqui ficam só os totais.
 */
@Tag(name = "Relatórios")
@RestController
@RequestMapping("/api/painel")
class PainelController {

    record Painel(Map<Situacao, Long> alunosPorSituacao, long presentesAgora, long entradasHoje) {
    }

    private final AlunoService alunos;
    private final AcessoService acessos;

    PainelController(AlunoService alunos, AcessoService acessos) {
        this.alunos = alunos;
        this.acessos = acessos;
    }

    @Operation(summary = "Totais de alunos por situação, presentes agora e entradas do dia")
    @GetMapping
    Painel painel() {
        return new Painel(alunos.contarPorSituacao(), acessos.totalPresentes(), acessos.entradasHoje());
    }
}
