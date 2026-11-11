package br.insper.provaFinal.avaliacao;

// FILA: remova apenas o /* e o */ que envolvem o código abaixo para ativar.
/*
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AvaliacaoFilaConfig {
    public static final String FILA = "avaliacoes.cadastro";
    public static final String FILA_ERROS = "avaliacoes.cadastro.erros";

    @Bean
    public Queue filaCadastroAvaliacao() {
        // Fila durável: continua existindo quando o broker reinicia.
        // Mensagens rejeitadas vão para a fila de erros via exchange padrão ("").
        return QueueBuilder.durable(FILA)
                .deadLetterExchange("")
                .deadLetterRoutingKey(FILA_ERROS)
                .build();
    }

    @Bean
    public Queue filaErrosCadastroAvaliacao() {
        return QueueBuilder.durable(FILA_ERROS).build();
    }

    @Bean
    public MessageConverter avaliacaoMessageConverter() {
        // Spring Boot 4 / Spring AMQP 4: conversor JSON baseado em Jackson 3.
        // O Boot aplica esse bean ao RabbitTemplate e ao listener padrão.
        return new JacksonJsonMessageConverter();
    }
}
*/
