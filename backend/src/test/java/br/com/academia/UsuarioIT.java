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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Gestão da equipe: papéis, regras de proteção e isolamento entre academias.
 * Mesmas propriedades dos outros ITs para reaproveitar o contexto Spring e o container.
 */
@SpringBootTest(properties = {
        "academia.admin.username=admin",
        "academia.admin.password=senha-forte-123"
})
@Import(TestcontainersConfig.class)
class UsuarioIT {

    @Autowired WebApplicationContext context;
    @Autowired JdbcClient jdbc;
    @Autowired PasswordEncoder encoder;

    MockMvc mvc;
    String admin;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        admin = login("admin", "senha-forte-123");
    }

    @Test
    void cadaPapelSoFazOQuePode() throws Exception {
        String recep = criarUsuario("recep", "RECEPCAO", "Recepcao#2026");
        String inst = criarUsuario("inst", "INSTRUTOR", "Instrutor#2026");
        String tokenRecep = login(recep, "Recepcao#2026");
        String tokenInst = login(inst, "Instrutor#2026");

        // Recepção cadastra aluno, mas não gerencia a equipe
        mvc.perform(post("/api/alunos").header("Authorization", "Bearer " + tokenRecep)
                        .contentType(MediaType.APPLICATION_JSON).content(aluno()))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + tokenRecep))
                .andExpect(status().isForbidden());

        // Instrutor só consulta: lista alunos, mas não cadastra nem registra catraca
        mvc.perform(get("/api/alunos").header("Authorization", "Bearer " + tokenInst))
                .andExpect(status().isOk());
        mvc.perform(post("/api/alunos").header("Authorization", "Bearer " + tokenInst)
                        .contentType(MediaType.APPLICATION_JSON).content(aluno()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/acessos").header("Authorization", "Bearer " + tokenInst)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rg\":\"8880001\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void respostaNuncaExpoeHashDaSenha() throws Exception {
        criarUsuario("hash", "RECEPCAO", "Recepcao#2026");
        String body = mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(body).doesNotContain("senha", "$2a$", "{bcrypt}");
    }

    @Test
    void validaUsernameUnicoESenhaFraca() throws Exception {
        String username = criarUsuario("dup", "RECEPCAO", "Recepcao#2026");

        cadastrar(username, "RECEPCAO", "OutraSenha#1").andExpect(status().isConflict());
        cadastrar(novoUsername("curta"), "RECEPCAO", "123").andExpect(status().isUnprocessableEntity());
        String u = novoUsername("contem");
        cadastrar(u, "RECEPCAO", u + "2026").andExpect(status().isUnprocessableEntity());
    }

    @Test
    void desativarBloqueiaLoginEReativarLibera() throws Exception {
        String username = criarUsuario("saiu", "RECEPCAO", "Recepcao#2026");
        Long id = idDe(username);
        login(username, "Recepcao#2026");

        mvc.perform(post("/api/usuarios/" + id + "/desativar").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
        loginStatus(username, "Recepcao#2026").andExpect(status().isUnauthorized());

        mvc.perform(post("/api/usuarios/" + id + "/reativar").header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.ativo").value(true));
        login(username, "Recepcao#2026");
    }

    @Test
    void academiaNuncaFicaSemAdministrador() throws Exception {
        Long idAdmin = idDe("admin");

        mvc.perform(post("/api/usuarios/" + idAdmin + "/desativar").header("Authorization", "Bearer " + admin))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(put("/api/usuarios/" + idAdmin).header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Administrador\",\"papel\":\"RECEPCAO\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void trocaDaPropriaSenhaExigeASenhaAtual() throws Exception {
        String username = criarUsuario("troca", "INSTRUTOR", "Instrutor#2026");
        String token = login(username, "Instrutor#2026");

        trocarSenha(token, "errada-123", "NovaSenha#2026").andExpect(status().isUnprocessableEntity());
        trocarSenha(token, "Instrutor#2026", "NovaSenha#2026").andExpect(status().isNoContent());

        loginStatus(username, "Instrutor#2026").andExpect(status().isUnauthorized());
        login(username, "NovaSenha#2026");
    }

    @Test
    void donoDeOutraAcademiaNaoEnxergaNemAlteraEstaEquipe() throws Exception {
        String recepA = criarUsuario("recepA", "RECEPCAO", "Recepcao#2026");
        Long idRecepA = idDe(recepA);
        String donoB = novaAcademiaComDono();

        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + donoB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username", not(hasItem(recepA))))
                .andExpect(jsonPath("$[*].username", not(hasItem("admin"))));
        mvc.perform(post("/api/usuarios/" + idRecepA + "/desativar").header("Authorization", "Bearer " + donoB))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/usuarios/" + idRecepA + "/redefinir-senha").header("Authorization", "Bearer " + donoB)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"novaSenha\":\"Invasor#2026\"}"))
                .andExpect(status().isNotFound());

        login(recepA, "Recepcao#2026"); // senha intacta
    }

    // ---------------------------------------------------------------- helpers

    private String criarUsuario(String prefixo, String papel, String senha) throws Exception {
        String username = novoUsername(prefixo);
        cadastrar(username, papel, senha).andExpect(status().isCreated());
        return username;
    }

    private ResultActions cadastrar(String username, String papel, String senha) throws Exception {
        return mvc.perform(post("/api/usuarios").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"nome\":\"Fulano\",\"papel\":\"%s\",\"senha\":\"%s\"}"
                        .formatted(username, papel, senha)));
    }

    private ResultActions trocarSenha(String token, String atual, String nova) throws Exception {
        return mvc.perform(put("/api/conta/senha").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"senhaAtual\":\"%s\",\"novaSenha\":\"%s\"}".formatted(atual, nova)));
    }

    private Long idDe(String username) throws Exception {
        String body = mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + admin))
                .andReturn().getResponse().getContentAsString();
        List<Number> ids = JsonPath.read(body, "$[?(@.username == '" + username + "')].id");
        return ids.getFirst().longValue();
    }

    private String novaAcademiaComDono() throws Exception {
        Long academiaId = jdbc.sql("INSERT INTO academia (nome) VALUES ('Academia Vizinha') RETURNING id")
                .query(Long.class).single();
        String username = novoUsername("donoB");
        jdbc.sql("""
                        INSERT INTO usuario (academia_id, username, senha_hash, nome, papel)
                        VALUES (:academia, :username, :hash, 'Dono B', 'ADMIN')""")
                .param("academia", academiaId).param("username", username)
                .param("hash", encoder.encode("SenhaDoB#2026")).update();
        return login(username, "SenhaDoB#2026");
    }

    private ResultActions loginStatus(String username, String senha) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, senha)));
    }

    private String login(String username, String senha) throws Exception {
        String body = loginStatus(username, senha).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private static String novoUsername(String prefixo) {
        return prefixo.toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 6);
    }

    private static String aluno() {
        return """
                {
                  "nome": "Carlos Recepcionado", "sexo": "MASCULINO", "rg": "8880001", "cpf": "52998224725",
                  "dataNascimento": "1988-02-11", "situacao": "ATIVO",
                  "endereco": {"cep": "66035-000", "logradouro": "Av. Nazaré", "numero": "10",
                               "bairro": "Nazaré", "cidade": "Belém", "estado": "PA"},
                  "celular": {"ddd": "91", "numero": "98888-1111"}
                }""";
    }
}
