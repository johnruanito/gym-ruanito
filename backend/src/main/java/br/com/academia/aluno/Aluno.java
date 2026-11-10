package br.com.academia.aluno;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "aluno")
public class Aluno {

    /** Chave técnica. A matrícula é única só dentro de cada academia, então não serve mais como PK. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Academia dona do registro. Preenchido pelo Hibernate no INSERT e usado como filtro automático
     * em toda consulta: o código de negócio nunca precisa passar academia_id à mão.
     */
    @TenantId
    @Column(name = "academia_id", nullable = false, updatable = false)
    private Long academiaId;

    @Column(name = "matricula", nullable = false, updatable = false, length = 10)
    private String matricula;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    // STRING em vez de ORDINAL: reordenar o enum não corrompe mais os dados gravados.
    @Enumerated(EnumType.STRING)
    @Column(name = "sexo", nullable = false, length = 10)
    private Sexo sexo;

    @Column(name = "rg", nullable = false, length = 20)
    private String rg;

    @Column(name = "cpf", nullable = false, length = 11)
    private String cpf;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao", nullable = false, length = 10)
    private Situacao situacao;

    @Column(name = "email", length = 120)
    private String email;

    @Embedded
    private Endereco endereco;

    @Embedded
    private Celular celular;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    /** Lock otimista: impede que duas recepcionistas sobrescrevam a edição uma da outra. */
    @Version
    @Column(name = "versao", nullable = false)
    private long versao;

    protected Aluno() {
        // JPA
    }

    public Aluno(String matricula, DadosAluno dados) {
        this.matricula = Objects.requireNonNull(matricula, "matricula");
        aplicar(dados);
    }

    public void atualizar(DadosAluno dados) {
        aplicar(dados);
    }

    private void aplicar(DadosAluno d) {
        this.nome = d.nome().strip();
        this.sexo = d.sexo();
        this.rg = d.rg();
        this.cpf = Cpf.normalizar(d.cpf());
        this.dataNascimento = d.dataNascimento();
        this.situacao = d.situacao();
        this.email = d.email();
        this.endereco = d.endereco();
        this.celular = d.celular();
    }

    @PrePersist
    void prePersist() {
        criadoEm = atualizadoEm = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = Instant.now();
    }

    public Long getId() { return id; }
    public Long getAcademiaId() { return academiaId; }
    public String getMatricula() { return matricula; }
    public String getNome() { return nome; }
    public Sexo getSexo() { return sexo; }
    public String getRg() { return rg; }
    public String getCpf() { return cpf; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public Situacao getSituacao() { return situacao; }
    public String getEmail() { return email; }
    public Endereco getEndereco() { return endereco; }
    public Celular getCelular() { return celular; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
    public long getVersao() { return versao; }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Aluno other && id != null && id.equals(other.id));
    }

    @Override
    public int hashCode() {
        return Aluno.class.hashCode();
    }
}
