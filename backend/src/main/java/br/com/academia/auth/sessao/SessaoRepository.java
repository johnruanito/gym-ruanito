package br.com.academia.auth.sessao;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SessaoRepository extends JpaRepository<Sessao, Long> {

    /**
     * Trava a linha: duas renovações simultâneas com o mesmo token são processadas uma após a outra,
     * nunca as duas "ao mesmo tempo" (o que geraria dois tokens válidos sem detecção).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Sessao s where s.tokenHash = :hash")
    Optional<Sessao> findByTokenHashForUpdate(@Param("hash") String hash);

    @Modifying
    @Query("update Sessao s set s.revogadoEm = :agora where s.familia = :familia and s.revogadoEm is null")
    int revogarFamilia(@Param("familia") UUID familia, @Param("agora") Instant agora);

    @Modifying
    @Query("update Sessao s set s.revogadoEm = :agora where s.usuarioId = :usuarioId and s.revogadoEm is null")
    int revogarDoUsuario(@Param("usuarioId") Long usuarioId, @Param("agora") Instant agora);

    @Modifying
    @Query("delete from Sessao s where s.expiraEm < :limite")
    int apagarExpiradasAntesDe(@Param("limite") Instant limite);
}
