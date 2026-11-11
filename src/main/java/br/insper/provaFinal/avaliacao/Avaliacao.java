package br.insper.provaFinal.avaliacao;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "avaliacoes")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Avaliacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private String emailAvaliador;

    @Column(nullable = false)
    private String emailAvaliado;

    @Column(nullable = false, length = 4000)
    private String comentarios;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal nota;

    @Column(nullable = false, updatable = false)
    private Instant dataAvaliacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20) default 'PENDENTE'")
    private StatusAvaliacao status = StatusAvaliacao.PENDENTE;

    public void alterarStatus(StatusAvaliacao status) {
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

    public Avaliacao(String emailAvaliador, CriarAvaliacaoRequest request) {
        this.emailAvaliador = emailAvaliador;
        this.emailAvaliado = request.emailAvaliado();
        this.comentarios = request.comentarios();
        this.nota = request.nota();
        this.dataAvaliacao = Instant.now();
    }
}
