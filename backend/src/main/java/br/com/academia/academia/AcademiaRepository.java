package br.com.academia.academia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AcademiaRepository extends JpaRepository<Academia, Long> {

    Optional<Academia> findFirstByOrderByIdAsc();
}
