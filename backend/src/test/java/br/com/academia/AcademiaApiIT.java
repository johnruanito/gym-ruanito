package br.com.academia;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo ponta a ponta contra PostgreSQL real (Testcontainers): login → cadastro → acesso → relatório.
 * Requer Docker.
 */
@SpringBootTest(properties = {
        "academia.admin.username=admin",
        "academia.admin.password=senha-forte-123"
})
@Import(TestcontainersConfig.class)
class AcademiaApiIT {

    @Autowired
    WebApplicationContext context;

    MockMvc mvc;
    String token;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        // O token vem em cookie HttpOnly; nos testes o usamos como Bearer (o resolver aceita os dois)
        token = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"senha-forte-123"}"""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("access_token").getValue();
    }

    @Test
    void semTokenRetorna401() throws Exception {
        mvc.perform(get("/api/alunos")).andExpect(status().isUnauthorized());
    }

    @Test
    void senhaErradaRetorna401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"errada"}"""))
                .andExpect(status().isUnauthorized());
    }

    /**
     * O payload do JWT é só Base64 (qualquer um lê). Ele deve levar apenas o necessário para
     * autorizar, sem dado pessoal. O nome continua disponível via /api/auth/me.
     */
    @Test
    void tokenNaoCarregaDadosPessoais() throws Exception {
        String payload = new String(java.util.Base64.getUrlDecoder().decode(token.split("\\.")[1]),
                java.nio.charset.StandardCharsets.UTF_8);

        org.assertj.core.api.Assertions.assertThat(payload)
                .contains("\"sub\"", "\"roles\"", "\"academia\"")
                .doesNotContain("\"nome\"", "Administrador");

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Administrador"))
                .andExpect(jsonPath("$.academia").value("Academia Principal"));
    }

    @Test
    void fluxoCompletoDeCadastroEControleDeAcesso() throws Exception {
        // cadastro com CPF que estourava o Integer no sistema antigo
        String criado = mvc.perform(post("/api/alunos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alunoJson("98765432100", "7654321", "ATIVO", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matricula").value(matchesPattern("\\d{8}")))
                .andExpect(jsonPath("$.cpf").value("98765432100"))
                .andReturn().getResponse().getContentAsString();
        String matricula = JsonPath.read(criado, "$.matricula");

        // CPF duplicado → 409
        mvc.perform(post("/api/alunos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alunoJson("98765432100", "1111111", "ATIVO", null)))
                .andExpect(status().isConflict());

        // pesquisa por trecho do nome, sem diferenciar maiúsculas
        mvc.perform(get("/api/alunos").param("nome", "SANTOS").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        // 1º registro = entrada, 2º = saída (pelo RG)
        mvc.perform(post("/api/acessos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"matricula\":\"" + matricula + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("ENTRADA"));
        mvc.perform(get("/api/acessos/presentes").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(post("/api/acessos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rg\":\"7654321\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("SAIDA"));

        mvc.perform(get("/api/acessos").param("matricula", matricula).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].saida").isNotEmpty());

        // aluno com histórico não pode ser excluído → 409
        mvc.perform(delete("/api/alunos/" + matricula).header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());

        // inativado: entrada bloqueada → 422
        String atual = mvc.perform(get("/api/alunos/" + matricula).header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        Number versao = JsonPath.read(atual, "$.versao");
        mvc.perform(put("/api/alunos/" + matricula).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alunoJson("98765432100", "7654321", "INATIVO", versao.longValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("INATIVO"));
        mvc.perform(post("/api/acessos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"matricula\":\"" + matricula + "\"}"))
                .andExpect(status().isUnprocessableEntity());

        // versão desatualizada → 409 (edição concorrente)
        mvc.perform(put("/api/alunos/" + matricula).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alunoJson("98765432100", "7654321", "ATIVO", versao.longValue())))
                .andExpect(status().isConflict());
    }

    @Test
    void validacaoRetornaErrosPorCampo() throws Exception {
        mvc.perform(post("/api/alunos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"A\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nome").exists())
                .andExpect(jsonPath("$.errors.cpf").exists());
    }

    private static String alunoJson(String cpf, String rg, String situacao, Long versao) {
        return """
                {
                  "nome": "Ana Paula Santos",
                  "sexo": "FEMININO",
                  "rg": "%s",
                  "cpf": "%s",
                  "dataNascimento": "1990-05-20",
                  "situacao": "%s",
                  "email": "ana@example.com",
                  "endereco": {"cep": "66035-000", "logradouro": "Av. Nazaré", "numero": "100",
                               "bairro": "Nazaré", "cidade": "Belém", "estado": "PA"},
                  "celular": {"ddd": "91", "numero": "98888-7777"},
                  "versao": %s
                }""".formatted(rg, cpf, situacao, versao);
    }
}
