package br.com.academia.shared.error;

/** Conflito com o estado atual do recurso (HTTP 409), ex.: CPF duplicado. */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String message) {
        super(message);
    }
}
