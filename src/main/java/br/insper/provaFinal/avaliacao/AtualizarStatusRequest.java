package br.insper.provaFinal.avaliacao;

import jakarta.validation.constraints.NotNull;

public record AtualizarStatusRequest(@NotNull StatusArtigo status) {}
