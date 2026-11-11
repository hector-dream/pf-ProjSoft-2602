package br.insper.provaFinal.avaliacao;

// FILA: remova apenas o /* e o */ que envolvem o código abaixo para ativar.
/*
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AvaliacaoFilaProducer {
    private final RabbitTemplate rabbitTemplate;

    public AvaliacaoFilaProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void enviar(CriarAvaliacaoRequest request, String emailAvaliador) {
        var mensagem = new AvaliacaoFilaMensagem(request, emailAvaliador);
        try {
            // Exchange padrão: o nome da fila é a routing key.
            rabbitTemplate.convertAndSend("", AvaliacaoFilaConfig.FILA, mensagem, message -> {
                message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                return message;
            });
        } catch (AmqpException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Não foi possível enviar a avaliação à fila. Tente novamente.", exception);
        }
    }
}
*/
