package br.com.academia.estado;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/**
 * Tabela de referência (UFs), populada por migração. No sistema antigo a chave String tinha
 * {@code @GeneratedValue}, o que não é suportado — agora a sigla é a chave natural.
 */
@Entity
@Immutable
@Table(name = "estado")
public class Estado {

    @Id
    @Column(name = "sigla", length = 2)
    private String sigla;

    @Column(name = "nome", nullable = false, length = 30)
    private String nome;

    protected Estado() {
    }

    public String getSigla() { return sigla; }
    public String getNome() { return nome; }
}
