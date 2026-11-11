package br.insper.provaFinal.avaliacao;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CriarArtigoRequest(
        @NotBlank @Email @Size(max = 255) String emailAvaliado,
        @NotBlank @Size(max = 4000) String comentarios,
        @NotNull @DecimalMin("0") @DecimalMax("10") @Digits(integer = 2, fraction = 2) BigDecimal nota
) {}
