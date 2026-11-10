package br.com.academia.acesso.web;

import br.com.academia.acesso.Acesso;
import br.com.academia.acesso.AcessoService;
import br.com.academia.shared.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "Controle de acesso")
@RestController
@RequestMapping("/api/acessos")
class AcessoController {

    record RegistroRequest(String matricula, String rg) {
    }

    record AcessoResponse(Long id, String matricula, String nome, Instant entrada, Instant saida, Long duracaoMinutos) {

        static AcessoResponse of(Acesso a) {
            var d = a.duracao();
            return new AcessoResponse(a.getId(), a.getAluno().getMatricula(), a.getAluno().getNome(),
                    a.getEntrada(), a.getSaida(), d == null ? null : d.toMinutes());
        }
    }

    private final AcessoService service;

    AcessoController(AcessoService service) {
        this.service = service;
    }

    @Operation(summary = "Registra entrada ou saída (alterna automaticamente) por matrícula ou RG")
    @PostMapping
    AcessoService.Registro registrar(@RequestBody RegistroRequest request) {
        return service.registrar(request.matricula(), request.rg());
    }

    @Operation(summary = "Relatório de entradas e saídas; período padrão: últimos 30 dias")
    @GetMapping
    PageResponse<AcessoResponse> relatorio(@RequestParam(required = false) String matricula,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "50") int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 200));
        return PageResponse.of(service.relatorio(matricula, de, ate, pageable), AcessoResponse::of);
    }

    @Operation(summary = "Alunos presentes na academia agora")
    @GetMapping("/presentes")
    List<AcessoResponse> presentes() {
        return service.presentes().stream().map(AcessoResponse::of).toList();
    }
}
