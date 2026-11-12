package br.com.academia.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

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

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected Usuario() {
    }

    public Usuario(Long academiaId, String username, String senhaHash, String nome, Papel papel) {
        this.academiaId = academiaId;
        this.username = username;
        this.senhaHash = senhaHash;
        this.nome = nome;
        this.papel = papel;
    }

    @PrePersist
    void prePersist() {
        criadoEm = Instant.now();
    }

    // --- Comportamento: alterações só por métodos com nome de negócio (sem setters genéricos) ---

    public void atualizar(String nome, Papel papel) {
        this.nome = nome;
        this.papel = papel;
    }

    public void desativar() {
        this.ativo = false;
    }

    public void reativar() {
        this.ativo = true;
    }

    /** Recebe o hash, nunca a senha: a entidade não sabe (nem deve saber) como criptografar. */
    public void trocarSenha(String novoHash) {
        this.senhaHash = novoHash;
    }

    public Long getId() { return id; }
    public Long getAcademiaId() { return academiaId; }
    public String getUsername() { return username; }
    public String getSenhaHash() { return senhaHash; }
    public String getNome() { return nome; }
    public Papel getPapel() { return papel; }
    public boolean isAtivo() { return ativo; }
    public Instant getCriadoEm() { return criadoEm; }
}
