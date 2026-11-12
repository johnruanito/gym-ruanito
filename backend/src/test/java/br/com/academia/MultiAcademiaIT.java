package br.com.academia;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Garante o isolamento entre academias: o requisito de segurança nº 1 de um SaaS.
 * Mesmas propriedades do {@link AcademiaApiIT} para reaproveitar o contexto e o container.
 */
@SpringBootTest(properties = {
        "academia.admin.username=admin",
        "academia.admin.password=senha-forte-123"
})
@Import(TestcontainersConfig.class)
class MultiAcademiaIT {

    @Autowired WebApplicationContext context;
    @Autowired JdbcClient jdbc;
    @Autowired PasswordEncoder encoder;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void academiaNaoVeNemAlteraDadosDeOutra() throws Exception {
        String tokenA = login("admin", "senha-forte-123");          // academia 1 (criada pela migração)
        String tokenB = novaAcademiaComUsuario("Academia B");

        // A cadastra um aluno
        String criado = mvc.perform(post("/api/alunos").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(aluno("Joana Isolada", "11144477735", "5550001")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String matriculaA = JsonPath.read(criado, "$.matricula");

        // B não encontra o aluno de A: nem por matrícula, nem na listagem, nem na catraca
        mvc.perform(get("/api/alunos/" + matriculaA).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/alunos").param("nome", "Joana Isolada").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(post("/api/acessos").header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rg\":\"5550001\"}"))
                .andExpect(status().isNotFound());

        // B pode cadastrar a mesma pessoa (mesmo CPF/RG): unicidade é por academia
        mvc.perform(post("/api/alunos").header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON).content(aluno("Joana Isolada", "11144477735", "5550001")))
                .andExpect(status().isCreated())
                // primeira matrícula do ano na academia B: o contador é por academia
                .andExpect(jsonPath("$.matricula").value(org.hamcrest.Matchers.endsWith("0001")));

        // Cada uma vê exatamente um "Joana Isolada" — o seu
        for (String token : new String[]{tokenA, tokenB}) {
            mvc.perform(get("/api/alunos").param("nome", "Joana Isolada").header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        // Painel de B não conta os alunos de A
        mvc.perform(get("/api/acessos/presentes").header("Authorization", "Bearer " + tokenB))
                .andExpect(jsonPath("$[*].matricula", everyItem(not(matriculaA))));
    }

    @Test
    void loginInformaAcademiaDoUsuario() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"senha-forte-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.academia").value("Academia Principal"));
    }

    /** Cria academia + usuário direto no banco (ainda não há API de cadastro de academia). */
    private String novaAcademiaComUsuario(String nome) throws Exception {
        Long academiaId = jdbc.sql("INSERT INTO academia (nome) VALUES (:nome) RETURNING id")
                .param("nome", nome).query(Long.class).single();
        String username = "dono-" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.sql("""
                        INSERT INTO usuario (academia_id, username, senha_hash, nome, papel)
                        VALUES (:academia, :username, :hash, 'Dono B', 'ADMIN')""")
                .param("academia", academiaId)
                .param("username", username)
                .param("hash", encoder.encode("senha-da-b-123"))
                .update();
        return login(username, "senha-da-b-123");
    }

    private String login(String username, String senha) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, senha)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private static String aluno(String nome, String cpf, String rg) {
        return """
                {
                  "nome": "%s", "sexo": "FEMININO", "rg": "%s", "cpf": "%s",
                  "dataNascimento": "1990-05-20", "situacao": "ATIVO",
                  "endereco": {"cep": "66035-000", "logradouro": "Av. Nazaré", "numero": "100",
                               "bairro": "Nazaré", "cidade": "Belém", "estado": "PA"},
                  "celular": {"ddd": "91", "numero": "98888-7777"}
                }""".formatted(nome, rg, cpf);
    }
}
