package br.com.academia.estado;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@Tag(name = "Domínios")
@RestController
@RequestMapping("/api/estados")
class EstadoController {

    record EstadoResponse(String sigla, String nome) {
    }

    private final EstadoRepository repository;

    EstadoController(EstadoRepository repository) {
        this.repository = repository;
    }

    /** Lista estática: o navegador pode manter em cache por um dia. */
    @GetMapping
    ResponseEntity<List<EstadoResponse>> listar() {
        var body = repository.findAllByOrderByNomeAsc().stream()
                .map(e -> new EstadoResponse(e.getSigla(), e.getNome()))
                .toList();
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePrivate()).body(body);
    }
}
