package br.com.academia;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sessão por cookies HttpOnly com refresh token rotativo. Aqui o "navegador" é simulado só com
 * cookies, sem cabeçalho Authorization.
 */
@SpringBootTest(properties = {
        "academia.admin.username=admin",
        "academia.admin.password=senha-forte-123"
})
@Import(TestcontainersConfig.class)
class SessaoIT {

    @Autowired WebApplicationContext context;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void loginGravaCookiesProtegidosESemTokenNoCorpo() throws Exception {
        MockHttpServletResponse res = login("admin", "senha-forte-123");

        MockCookie access = (MockCookie) res.getCookie("access_token");
        MockCookie refresh = (MockCookie) res.getCookie("refresh_token");
        for (MockCookie c : new MockCookie[]{access, refresh}) {
            assertThat(c.isHttpOnly()).as("HttpOnly em " + c.getName()).isTrue();
            assertThat(c.getSecure()).as("Secure em " + c.getName()).isTrue();
            assertThat(c.getSameSite()).isEqualTo("Strict");
        }
        assertThat(access.getPath()).isEqualTo("/api");
        assertThat(refresh.getPath()).isEqualTo("/api/auth"); // refresh nunca vai para o resto da API

        // JWT começa com "eyJ"; o refresh não aparece em lugar nenhum do corpo
        assertThat(res.getContentAsString()).doesNotContain("eyJ", refresh.getValue())
                .contains("\"perfil\"", "Academia Principal");
        assertThat(res.getHeader("Cache-Control")).isEqualTo("no-store");
    }

    @Test
    void navegadorSeAutenticaSoComCookie() throws Exception {
        Cookie access = login("admin", "senha-forte-123").getCookie("access_token");

        mvc.perform(get("/api/alunos").cookie(access)).andExpect(status().isOk());
        mvc.perform(get("/api/alunos")).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRotacionaETokenReutilizadoDerrubaAFamiliaInteira() throws Exception {
        Cookie r1 = login("admin", "senha-forte-123").getCookie("refresh_token");

        MockHttpServletResponse renovado = refresh(r1).andExpect(status().isOk()).andReturn().getResponse();
        Cookie r2 = renovado.getCookie("refresh_token");
        assertThat(r2.getValue()).isNotEqualTo(r1.getValue());
        mvc.perform(get("/api/alunos").cookie(renovado.getCookie("access_token"))).andExpect(status().isOk());

        // Alguém reaproveita o token antigo (copiado de um computador infectado, por exemplo)
        MockHttpServletResponse reuso = refresh(r1).andExpect(status().isUnauthorized()).andReturn().getResponse();
        assertThat(reuso.getCookie("refresh_token").getMaxAge()).isZero(); // cookies apagados

        // Resultado: o token legítimo mais recente também morreu — a família toda foi revogada
        refresh(r2).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutEncerraASessaoNoServidor() throws Exception {
        Cookie r = login("admin", "senha-forte-123").getCookie("refresh_token");

        MockHttpServletResponse saida = mvc.perform(post("/api/auth/logout").cookie(r))
                .andExpect(status().isNoContent()).andReturn().getResponse();
        assertThat(saida.getCookie("access_token").getMaxAge()).isZero();

        // Mesmo que alguém tenha guardado o cookie, ele não vale mais
        refresh(r).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshSemCookieRetorna401() throws Exception {
        mvc.perform(post("/api/auth/refresh")).andExpect(status().isUnauthorized());
    }

    @Test
    void desativarUsuarioDerrubaASessaoDeleNaHora() throws Exception {
        String admin = login("admin", "senha-forte-123").getCookie("access_token").getValue();
        String username = "sessao-" + UUID.randomUUID().toString().substring(0, 6);
        mvc.perform(post("/api/usuarios").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"nome\":\"Temp\",\"papel\":\"RECEPCAO\",\"senha\":\"Recepcao#2026\"}"
                                .formatted(username)))
                .andExpect(status().isCreated());
        Cookie refreshDoFuncionario = login(username, "Recepcao#2026").getCookie("refresh_token");
        String equipe = mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + admin))
                .andReturn().getResponse().getContentAsString();
        java.util.List<Number> ids = com.jayway.jsonpath.JsonPath.read(equipe, "$[?(@.username == '" + username + "')].id");
        Long id = ids.getFirst().longValue();

        mvc.perform(post("/api/usuarios/" + id + "/desativar").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());

        refresh(refreshDoFuncionario).andExpect(status().isUnauthorized());
    }

    @Test
    void trocarAPropriaSenhaEncerraAsSessoes() throws Exception {
        String admin = login("admin", "senha-forte-123").getCookie("access_token").getValue();
        String username = "troca-" + UUID.randomUUID().toString().substring(0, 6);
        mvc.perform(post("/api/usuarios").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"nome\":\"Temp\",\"papel\":\"INSTRUTOR\",\"senha\":\"Instrutor#2026\"}"
                        .formatted(username)));
        MockHttpServletResponse sessao = login(username, "Instrutor#2026");

        mvc.perform(put("/api/conta/senha").cookie(sessao.getCookie("access_token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"Instrutor#2026\",\"novaSenha\":\"NovaSenha#2026\"}"))
                .andExpect(status().isNoContent());

        refresh(sessao.getCookie("refresh_token")).andExpect(status().isUnauthorized());
    }

    private MockHttpServletResponse login(String username, String senha) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, senha)))
                .andExpect(status().isOk())
                .andReturn().getResponse();
    }

    private org.springframework.test.web.servlet.ResultActions refresh(Cookie refreshToken) throws Exception {
        return mvc.perform(post("/api/auth/refresh").cookie(refreshToken));
    }
}
