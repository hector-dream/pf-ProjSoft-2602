package br.insper.provaFinal.artigo;

import jakarta.validation.constraints.NotNull;

public record AtualizarStatusRequest(@NotNull StatusArtigo status) {}
