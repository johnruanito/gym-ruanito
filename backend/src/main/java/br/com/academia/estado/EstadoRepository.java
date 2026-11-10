package br.com.academia.estado;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EstadoRepository extends JpaRepository<Estado, String> {

    List<Estado> findAllByOrderByNomeAsc();
}
