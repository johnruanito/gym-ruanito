package br.com.academia.shared.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

/**
 * Ponte entre o Hibernate e o {@link TenantContext}.
 * <p>
 * Toda vez que o Hibernate abre uma sessão, ele pergunta "qual é o tenant atual?" a esta classe.
 * Com a resposta, ele:
 * <ul>
 *   <li>acrescenta {@code WHERE academia_id = ?} em toda consulta de entidades com {@code @TenantId};</li>
 *   <li>preenche {@code academia_id} automaticamente ao inserir.</li>
 * </ul>
 * Registrada em {@code application.yml} (hibernate.tenant_identifier_resolver). Precisa de construtor
 * público sem argumentos, porque é o Hibernate que cria a instância.
 */
public class TenantResolver implements CurrentTenantIdentifierResolver<Long> {

    @Override
    public Long resolveCurrentTenantIdentifier() {
        return TenantContext.atual();
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
