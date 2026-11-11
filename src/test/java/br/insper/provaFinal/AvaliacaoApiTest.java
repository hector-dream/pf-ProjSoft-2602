package br.insper.provaFinal;

import br.insper.provaFinal.avaliacao.AvaliacaoRepository;
import br.insper.provaFinal.avaliacao.Avaliacao;
import br.insper.provaFinal.avaliacao.CriarAvaliacaoRequest;
import br.insper.provaFinal.avaliacao.StatusAvaliacao;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:avaliacoes;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class AvaliacaoApiTest {
    @Autowired MockMvc mvc;
    @Autowired AvaliacaoRepository repository;
    @MockitoBean JwtDecoder decoder;

    @BeforeEach
    void setup() {
        repository.deleteAll();
        token("admin", List.of("ADMIN"), "admin@empresa.com");
        token("user", List.of("USER"), "user@empresa.com");
        token("sem-papel", List.of(), "user@empresa.com");
        token("sem-email", List.of("ADMIN"), null);
    }

    private void token(String value, List<String> roles, String email) {
        var builder = Jwt.withTokenValue(value).header("alg", "RS256")
                .subject("auth0|" + value).claim("https://prova-api/roles", roles);
        if (email != null) builder.claim("https://prova-api/email", email);
        when(decoder.decode(value)).thenReturn(builder.build());
    }

    private String body(String nota) {
        return """
                {"emailAvaliado":"colega@empresa.com","comentarios":"Bom trabalho","nota":%s}
                """.formatted(nota);
    }

    @Test
    void adminCadastraEPersiste() throws Exception {
        mvc.perform(post("/avaliacoes").header("Authorization", "Bearer admin")
                        .contentType(MediaType.APPLICATION_JSON).content(body("8.5")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.emailAvaliador").value("admin@empresa.com"))
                .andExpect(jsonPath("$.emailAvaliado").value("colega@empresa.com"))
                .andExpect(jsonPath("$.nota").value(8.5))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.dataAvaliacao").isNotEmpty());
        assertThat(repository.findAll()).hasSize(1);
        mvc.perform(get("/avaliacoes").header("Authorization", "Bearer user"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"admin", "user"})
    void adminEUserListam(String token) throws Exception {
        mvc.perform(get("/avaliacoes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void userNaoCadastra() throws Exception {
        mvc.perform(post("/avaliacoes").header("Authorization", "Bearer user")
                        .contentType(MediaType.APPLICATION_JSON).content(body("8")))
                .andExpect(status().isForbidden());
        assertThat(repository.count()).isZero();
    }

    @Test
    void semTokenRetorna401() throws Exception {
        mvc.perform(get("/avaliacoes")).andExpect(status().isUnauthorized());
        mvc.perform(post("/avaliacoes").contentType(MediaType.APPLICATION_JSON).content(body("8")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalidoRetorna401() throws Exception {
        when(decoder.decode("invalido")).thenThrow(new BadJwtException("Token invalido"));
        mvc.perform(get("/avaliacoes").header("Authorization", "Bearer invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void semPapelOuEmailRetorna403() throws Exception {
        mvc.perform(get("/avaliacoes").header("Authorization", "Bearer sem-papel"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/avaliacoes").header("Authorization", "Bearer sem-email")
                        .contentType(MediaType.APPLICATION_JSON).content(body("8")))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "10.01", "null", "1.234"})
    void rejeitaNotasInvalidas(String nota) throws Exception {
        mvc.perform(post("/avaliacoes").header("Authorization", "Bearer admin")
                        .contentType(MediaType.APPLICATION_JSON).content(body(nota)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros.nota").exists());
        assertThat(repository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "10"})
    void aceitaLimitesDaNota(String nota) throws Exception {
        mvc.perform(post("/avaliacoes").header("Authorization", "Bearer admin")
                        .contentType(MediaType.APPLICATION_JSON).content(body(nota)))
                .andExpect(status().isCreated());
    }

    @Test
    void adminAlteraStatusEPersiste() throws Exception {
        Long id = cadastrarAvaliacao();
        for (String status : List.of("VISTO", "PENDENTE")) {
            mvc.perform(patch("/avaliacoes/{id}/status", id)
                            .header("Authorization", "Bearer admin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"" + status + "\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(status));
            assertThat(repository.findById(id).orElseThrow().getStatus())
                    .isEqualTo(StatusAvaliacao.valueOf(status));
        }
    }

    @Test
    void protegeAlteracaoDeStatus() throws Exception {
        Long id = cadastrarAvaliacao();
        mvc.perform(patch("/avaliacoes/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"VISTO\"}"))
                .andExpect(status().isUnauthorized());
        for (String token : List.of("user", "sem-papel")) {
            mvc.perform(patch("/avaliacoes/{id}/status", id)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"VISTO\"}"))
                    .andExpect(status().isForbidden());
        }
        assertThat(repository.findById(id).orElseThrow().getStatus()).isEqualTo(StatusAvaliacao.PENDENTE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"status\":null}", "{\"status\":\"INVALIDO\"}"})
    void rejeitaStatusInvalido(String body) throws Exception {
        Long id = cadastrarAvaliacao();
        mvc.perform(patch("/avaliacoes/{id}/status", id)
                        .header("Authorization", "Bearer admin")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        assertThat(repository.findById(id).orElseThrow().getStatus()).isEqualTo(StatusAvaliacao.PENDENTE);
    }

    @Test
    void atualizarStatusInexistenteRetorna404() throws Exception {
        mvc.perform(patch("/avaliacoes/999999/status").header("Authorization", "Bearer admin")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"VISTO\"}"))
                .andExpect(status().isNotFound());
    }

    private Long cadastrarAvaliacao() {
        return repository.save(new Avaliacao("admin@empresa.com",
                new CriarAvaliacaoRequest("colega@empresa.com", "Bom trabalho", new BigDecimal("8.5"))))
                .getId();
    }

    @ParameterizedTest
    @ValueSource(strings = {"admin", "user"})
    void buscaPorId(String token) throws Exception {
        Long id = cadastrarAvaliacao();
        mvc.perform(get("/avaliacoes/{id}", id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.emailAvaliado").value("colega@empresa.com"));
    }

    @Test
    void adminExclui() throws Exception {
        Long id = cadastrarAvaliacao();
        mvc.perform(delete("/avaliacoes/{id}", id).header("Authorization", "Bearer admin"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        assertThat(repository.findById(id)).isEmpty();
        mvc.perform(get("/avaliacoes/{id}", id).header("Authorization", "Bearer admin"))
                .andExpect(status().isNotFound());
    }

    @Test
    void idInexistenteRetorna404() throws Exception {
        mvc.perform(get("/avaliacoes/999999").header("Authorization", "Bearer user"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/avaliacoes/999999").header("Authorization", "Bearer admin"))
                .andExpect(status().isNotFound());
    }

    @Test
    void protegeRotasPorId() throws Exception {
        Long id = cadastrarAvaliacao();
        mvc.perform(get("/avaliacoes/{id}", id)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/avaliacoes/{id}", id)).andExpect(status().isUnauthorized());
        mvc.perform(get("/avaliacoes/{id}", id).header("Authorization", "Bearer sem-papel"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/avaliacoes/{id}", id).header("Authorization", "Bearer user"))
                .andExpect(status().isForbidden());
        assertThat(repository.existsById(id)).isTrue();
    }

    @Test
    void rejeitaCamposInvalidos() throws Exception {
        mvc.perform(post("/avaliacoes").header("Authorization", "Bearer admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emailAvaliado\":\"invalido\",\"comentarios\":\" \",\"nota\":8}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.emailAvaliado").exists())
                .andExpect(jsonPath("$.erros.comentarios").exists());
    }
}
