package br.insper.provaFinal.avaliacao;

import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/avaliacoes")
public class AvaliacaoController {
    private final AvaliacaoService service;
    private final Validator validator;
    private final String emailClaim;

    // FILA: descomente este campo e use o construtor alternativo abaixo.
    // private final AvaliacaoFilaProducer filaProducer;

    public AvaliacaoController(AvaliacaoService service, Validator validator,
            @Value("${auth0.email-claim}") String emailClaim) {
        this.service = service;
        this.validator = validator;
        this.emailClaim = emailClaim;
    }

    // FILA: comente o construtor atual inteiro e descomente este bloco.
    // public AvaliacaoController(AvaliacaoService service, Validator validator,
    //         @Value("${auth0.email-claim}") String emailClaim, AvaliacaoFilaProducer filaProducer) {
    //     this.service = service;
    //     this.validator = validator;
    //     this.emailClaim = emailClaim;
    //     this.filaProducer = filaProducer;
    // }

    @PostMapping
    // FILA: troque CREATED por ACCEPTED (202: aceitou para processamento, ainda não salvou).
    @ResponseStatus(HttpStatus.CREATED)
    // FILA: comente a assinatura abaixo e descomente esta alternativa; mantenha a linha do Jwt.
    // public java.util.Map<String, String> cadastrar(@Valid @RequestBody CriarAvaliacaoRequest request,
    public Avaliacao cadastrar(@Valid @RequestBody CriarAvaliacaoRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString(emailClaim);
        if (email == null) {
            email = jwt.getClaimAsString("email");
        }
        if (!validator.validate(new EmailAvaliador(email)).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "O access token deve conter o e-mail do avaliador.");
        }
        // CADASTRO DIRETO (ativo). FILA: comente este return e descomente as duas linhas abaixo.
        return service.cadastrar(request, email);
        // filaProducer.enviar(request, email);
        // return java.util.Map.of("mensagem", "Avaliação enviada para processamento.");
    }

    @GetMapping
    public List<Avaliacao> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Avaliacao buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }

    @PatchMapping("/{id}/status")
    public Avaliacao atualizarStatus(@PathVariable Long id,
            @Valid @RequestBody AtualizarStatusRequest request) {
        return service.atualizarStatus(id, request);
    }

    private record EmailAvaliador(@NotBlank @Email @Size(max = 255) String email) {}
}
