package br.insper.provaFinal.artigo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;

//título, descrição, prioridade e e-mail do usuário que criou o artigo

@Entity
@Table(name = "artigos")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Artigo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private String emailCriador;

    @Column(nullable = false, length = 4000)
    private String descricao;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal nota;

    @Column(nullable = false, updatable = false)
    private Instant dataArtigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20) default 'MINIMA'")
    private StatusArtigo status = StatusArtigo.MINIMA;

    public void alterarStatus(StatusArtigo status) {
        this.status = status;
    }

    // SOFT DELETE (1/3): descomente o campo e o método abaixo.
    // O default false também preenche registros antigos ao criar a coluna no PostgreSQL.
    // JsonIgnore impede que o campo interno apareça no JSON da API.
    // @com.fasterxml.jackson.annotation.JsonIgnore
    // @Column(nullable = false, columnDefinition = "boolean default false")
    // private boolean excluida = false;
    //
    // public void marcarComoExcluida() {
    //     this.excluida = true;
    // }

    public Artigo(String emailAvaliador, CriarArtigoRequest request) {
        this.emailCriador = emailAvaliador;
        this.titulo = request.emailAvaliado();
        this.descricao = request.comentarios();
        this.nota = request.nota();
        this.dataArtigo = Instant.now();
    }
}
