package br.com.academia.aluno.web;

import br.com.academia.aluno.AlunoFiltro;
import br.com.academia.aluno.AlunoService;
import br.com.academia.aluno.Situacao;
import br.com.academia.shared.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Tag(name = "Alunos")
@RestController
@RequestMapping("/api/alunos")
class AlunoController {

    private static final int MAX_PAGE_SIZE = 100;

    private final AlunoService service;

    AlunoController(AlunoService service) {
        this.service = service;
    }

    @Operation(summary = "Pesquisa alunos por critérios opcionais, paginada e ordenada por nome")
    @GetMapping
    PageResponse<AlunoResumo> pesquisar(@RequestParam(required = false) String matricula,
                                        @RequestParam(required = false) String nome,
                                        @RequestParam(required = false) String rg,
                                        @RequestParam(required = false) String cpf,
                                        @RequestParam(required = false) String celular,
                                        @RequestParam(required = false) Situacao situacao,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        var filtro = new AlunoFiltro(matricula, nome, rg, cpf, celular, situacao);
        var pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE), Sort.by("nome"));
        return PageResponse.of(service.pesquisar(filtro, pageable), AlunoResumo::of);
    }

    @GetMapping("/{matricula}")
    AlunoResponse buscar(@PathVariable String matricula) {
        return AlunoResponse.of(service.buscar(matricula));
    }

    /** Escrita: dono e recepção. O instrutor só consulta. */
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCAO')")
    @PostMapping
    ResponseEntity<AlunoResponse> cadastrar(@Valid @RequestBody AlunoRequest request) {
        var aluno = service.cadastrar(request.toDados());
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{matricula}").buildAndExpand(aluno.getMatricula()).toUri();
        return ResponseEntity.created(location).body(AlunoResponse.of(aluno));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCAO')")
    @PutMapping("/{matricula}")
    AlunoResponse atualizar(@PathVariable String matricula, @Valid @RequestBody AlunoRequest request) {
        return AlunoResponse.of(service.atualizar(matricula, request.toDados(), request.versao()));
    }

    @Operation(summary = "Exclui aluno sem histórico de acessos (somente ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{matricula}")
    ResponseEntity<Void> excluir(@PathVariable String matricula) {
        service.excluir(matricula);
        return ResponseEntity.noContent().build();
    }
}
