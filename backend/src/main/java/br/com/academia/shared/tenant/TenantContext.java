package br.com.academia.shared.tenant;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * De qual academia é a requisição atual.
 * <p>
 * A resposta vem do JWT do usuário logado (claim {@code academia}), que o servidor assinou no login:
 * o cliente não consegue trocar de academia mexendo em parâmetro ou cabeçalho.
 */
public final class TenantContext {

    /** Nome do claim do JWT que carrega o id da academia. */
    public static final String CLAIM = "academia";

    /**
     * Usado quando não há usuário autenticado (start da aplicação, tela de login).
     * Não existe academia com esse id: consultas retornam vazio e inserções falham na FK.
     */
    public static final long NENHUMA = -1L;

    private TenantContext() {
    }

    /** Id da academia do usuário autenticado, ou {@link #NENHUMA}. */
    public static long atual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt && jwt.getToken().getClaim(CLAIM) instanceof Number id) {
            return id.longValue();
        }
        return NENHUMA;
    }

    /** Como {@link #atual()}, mas falha se não houver academia (uso em operações que exigem uma). */
    public static long exigir() {
        long id = atual();
        if (id == NENHUMA) {
            throw new IllegalStateException("Operação exige um usuário autenticado vinculado a uma academia");
        }
        return id;
    }
}
