package br.com.academia.aluno;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Year;

/**
 * Gera matrículas sem condição de corrida.
 * <p>
 * O sistema antigo fazia {@code SELECT MAX(matricula)} + 1: dois cadastros simultâneos liam o mesmo
 * máximo e geravam a mesma chave primária. Aqui um contador por ano é incrementado com um único
 * {@code INSERT ... ON CONFLICT DO UPDATE ... RETURNING}, que é atômico no PostgreSQL.
 */
@Component
class MatriculaGenerator {

    private final JdbcClient jdbc;
    private final Clock clock;

    MatriculaGenerator(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String proxima() {
        Year ano = Year.now(clock);
        Integer sequencial = jdbc.sql("""
                        INSERT INTO matricula_sequencia (ano, ultimo) VALUES (:ano, 1)
                        ON CONFLICT (ano) DO UPDATE SET ultimo = matricula_sequencia.ultimo + 1
                        RETURNING ultimo
                        """)
                .param("ano", ano.getValue())
                .query(Integer.class)
                .single();
        return Matricula.formatar(ano, sequencial);
    }
}
