package br.com.academia.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * ATENÇÃO: {@link Usuario} não tem {@code @TenantId} (o login precisa achar o usuário antes de saber
 * a academia). Por isso aqui NÃO existe filtro automático: toda consulta da gestão de equipe recebe
 * o {@code academiaId} explicitamente. Esquecer esse parâmetro = um dono ver a equipe de outra academia.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Só para o login (username é único na plataforma inteira). */
    Optional<Usuario> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    List<Usuario> findByAcademiaIdOrderByNomeAsc(Long academiaId);

    Optional<Usuario> findByIdAndAcademiaId(Long id, Long academiaId);

    Optional<Usuario> findByUsernameIgnoreCaseAndAcademiaId(String username, Long academiaId);

    long countByAcademiaIdAndPapelAndAtivoTrue(Long academiaId, Papel papel);
}
