package br.com.academia.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Academia à qual o usuário pertence. Sem {@code @TenantId} de propósito: no login ainda não
     * sabemos a academia, e é justamente o usuário que nos diz qual é.
     */
    @Column(name = "academia_id", nullable = false, updatable = false)
    private Long academiaId;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    /** Hash BCrypt — nunca a senha em texto. */
    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "papel", nullable = false, length = 20)
    private Papel papel;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    protected Usuario() {
    }

    public Usuario(Long academiaId, String username, String senhaHash, String nome, Papel papel) {
        this.academiaId = academiaId;
        this.username = username;
        this.senhaHash = senhaHash;
        this.nome = nome;
        this.papel = papel;
    }

    public Long getId() { return id; }
    public Long getAcademiaId() { return academiaId; }
    public String getUsername() { return username; }
    public String getSenhaHash() { return senhaHash; }
    public String getNome() { return nome; }
    public Papel getPapel() { return papel; }
    public boolean isAtivo() { return ativo; }
}
