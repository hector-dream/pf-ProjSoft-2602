package br.insper.provaFinal.avaliacao;

// FILA: remova apenas o /* e o */ que envolvem o código abaixo para ativar.
/*
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AvaliacaoFilaConsumer {
    private final AvaliacaoService service;

    public AvaliacaoFilaConsumer(AvaliacaoService service) {
        this.service = service;
    }

    @RabbitListener(queues = AvaliacaoFilaConfig.FILA)
    public void receber(AvaliacaoFilaMensagem mensagem) {
        // Reutiliza o método transacional que já salva no PostgreSQL.
        // NÃO chame o producer aqui: isso enviaria a mensagem de volta à fila.
        // Sem capturar a exceção: falhas precisam chegar ao listener para retry/DLQ.
        service.cadastrar(mensagem.request(), mensagem.emailAvaliador());
    }
}
*/
