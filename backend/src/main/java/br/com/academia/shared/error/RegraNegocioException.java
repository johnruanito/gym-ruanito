package br.com.academia.shared.error;

/**
 * Violação de regra de negócio (HTTP 422). Substitui a antiga ValidationException do EJB.
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String message) {
        super(message);
    }
}
