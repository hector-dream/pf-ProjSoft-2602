# Cadastro por fila — alternativa comentada

O cadastro direto continua ativo: POST → controller → service → PostgreSQL, com resposta 201.
Nada foi instalado no servidor nem ativado em produção.

A alternativa usa RabbitMQ e Spring AMQP:

```text
POST /avaliacoes (ADMIN, validado pelo Auth0 e Bean Validation)
  → producer publica dados + e-mail do avaliador no RabbitMQ
  → HTTP 202 com mensagem de recebimento
  → consumer recebe a mensagem e chama o service
  → service salva a avaliação no PostgreSQL
```

A fila é externa à aplicação, não uma lista Java em memória. O mesmo processo Java
contém o producer e o consumer; o RabbitMQ é um serviço separado.
O GET continua consultando o PostgreSQL, não as mensagens que aguardam processamento.

## Arquivos preparados

Na pasta `src/main/java/br/insper/provaFinal/avaliacao/`:

| Arquivo | Responsabilidade |
| --- | --- |
| `AvaliacaoFilaConfig.java` | Declara fila durável, fila de erros e conversor JSON |
| `AvaliacaoFilaMensagem.java` | Transporta o request e o e-mail autenticado |
| `AvaliacaoFilaProducer.java` | Publica a mensagem em vez de gravar no banco pelo POST |
| `AvaliacaoFilaConsumer.java` | Usa `@RabbitListener` e chama o cadastro transacional do service |
| `AvaliacaoController.java` | Tem campo, construtor, assinatura e retorno alternativos comentados |

As quatro classes de fila têm o código envolvido por um comentário `/* ... */`.
Para ativá-las, remova apenas o delimitador de abertura e o de fechamento que envolvem
o código de cada arquivo; mantenha o package e os comentários explicativos.

## Ativação, passo a passo

1. Em `pom.xml`, descomente somente a dependência `spring-boot-starter-amqp` marcada `FILA`.
   Mantenha as demais dependências e recarregue o Maven na IDE.
2. Ative as quatro classes indicadas acima removendo os delimitadores de comentário.
3. Em `application.properties`, descomente as propriedades `spring.rabbitmq.*` no final.
4. No `AvaliacaoController`, siga os comentários `FILA`:
   - Descomente o campo `filaProducer`.
   - Comente o construtor atual completo e descomente o construtor alternativo que recebe o producer.
   - No POST, troque `HttpStatus.CREATED` por `HttpStatus.ACCEPTED`.
   - Comente a linha da assinatura com retorno `Avaliacao` e descomente a alternativa
     com retorno `java.util.Map<String, String>`. A linha seguinte com `Jwt jwt` continua igual.
   - Mantenha toda a validação e extração do e-mail do JWT.
   - Comente `return service.cadastrar(request, email);`.
   - Descomente `filaProducer.enviar(request, email);` e o return com a mensagem.
5. **Não altere `AvaliacaoService.cadastrar`: o consumer precisa desse método para salvar.**
   Não deixe o POST gravar diretamente e publicar ao mesmo tempo: isso cadastraria duas vezes.
6. Suba o RabbitMQ, configure a conexão, reinicie a aplicação e adapte os testes e o front.

O POST permanece restrito a ADMIN: nada muda em `SecurityConfig`, Auth0, roles ou URLs.
GET, PATCH e DELETE também permanecem como estão. O soft delete é independente da fila.

## RabbitMQ local com Docker

Execute uma vez, com Docker em execução. São credenciais de exemplo para estudo local:

```powershell
docker run -d --name rabbitmq-prova -p 127.0.0.1:5672:5672 -p 127.0.0.1:15672:15672 -e RABBITMQ_DEFAULT_USER=prova -e RABBITMQ_DEFAULT_PASS=prova-local -v rabbitmq-prova-dados:/var/lib/rabbitmq rabbitmq:4-management
```

Se o contêiner já existe e está parado, use `docker start rabbitmq-prova`, em vez de
tentar criá-lo novamente com o mesmo nome.

- Porta 5672: comunicação da aplicação com o broker.
- Painel: `http://localhost:15672`, usuário `prova`, senha `prova-local`.
- As propriedades comentadas já usam esses defaults para execução local do Java.
- A fila `avaliacoes.cadastro` e a fila `avaliacoes.cadastro.erros` são declaradas
  automaticamente quando a aplicação se conecta ao broker.
- Fila durável, mensagens persistentes e volume ajudam a manter mensagens após reinícios.

## Na AWS

O RabbitMQ precisa estar acessível pelo contêiner Java. Se estiver na mesma EC2,
conecte-o à rede Docker `rede`, use volume persistente e configure usuário/senha próprios.
Não é necessário publicar as portas do RabbitMQ na internet para o Java acessá-lo nessa rede.

No `docker run` do Java no workflow, adicione as variáveis de conexão, por exemplo:

```yaml
            -e RABBITMQ_HOST=rabbitmq-prova \
            -e RABBITMQ_USER='${{ secrets.RABBITMQ_USER }}' \
            -e RABBITMQ_PASSWORD='${{ secrets.RABBITMQ_PASSWORD }}' \
```

O nome `rabbitmq-prova` deve coincidir com o nome do contêiner do broker na rede `rede`.
`localhost` dentro do contêiner Java significa o próprio contêiner, não o RabbitMQ.
Crie os secrets correspondentes no GitHub; o workflow atual não sobe um broker nem
envia essas variáveis automaticamente. A porta padrão permanece 5672.
Depois publique a versão com os comentários removidos.

## Resposta do POST e frontend

Com fila, o POST retorna **202 Accepted**, não a avaliação salva com ID:

```json
{ "mensagem": "Avaliação enviada para processamento." }
```

Em `front-pf-ProjSoft-26.2/src/App.jsx`, dentro de `cadastrar`, troque a mensagem
`Avaliação cadastrada.` por `Avaliação enviada para processamento. Atualize a lista em instantes.`
O comentário `FILA` mostra o ponto exato. O helper `src/api.js` já aceita 202 com JSON.

A atualização imediata da lista pode ocorrer antes de o consumer terminar. O botão
`Atualizar lista` permite consultar novamente. Não espere receber o ID no POST nem
afirme que foi gravado só porque recebeu 202. Para uma tela que acompanhe processamento,
seria necessário acrescentar um identificador de solicitação e uma consulta de situação.

`PENDENTE`/`VISTO` continuam sendo o status da avaliação **já salva**: não representam
o estado da fila. Nesta alternativa, a data da avaliação é gerada pelo service/entidade
quando o consumer grava; se a prova exigir a data do envio, inclua-a na mensagem e
adapte o construtor da entidade para preservar essa data.

## Testes ao ativar

Os testes atuais verificam o cadastro síncrono com 201 e persistência imediata.
Eles precisam ser adaptados ao ativar a fila; não basta mudar 201 para 202.

- Para testar a API sem broker, use `@MockitoBean AvaliacaoFilaProducer` e configure
  `spring.rabbitmq.listener.simple.auto-startup=false` no teste. Verifique 202, a mensagem
  e que `enviar(request, emailDoToken)` foi chamado; o POST não deve gravar no repository.
- Mantenha os testes de validação: requests inválidos não devem publicar mensagens.
- Mantenha 401/403: usuário sem token ou USER não deve conseguir publicar.
- Teste separadamente que o consumer chama o service e que o cadastro persiste os dados.
- Teste com RabbitMQ real para validar serialização JSON, conexão, recebimento e gravação.
- Para observar o efeito assíncrono, inicie a aplicação com
  `spring.rabbitmq.listener.simple.auto-startup=false`, faça um POST e veja a mensagem
  na fila. Depois reinicie com o listener habilitado e confira a gravação no GET.

Um POST sem token continua retornando 401. Falha de publicação detectada pelo producer
retorna 503. Falha ao gravar **depois do 202** não pode alterar a resposta HTTP já enviada:
confira os logs e a fila de erros.

## Tentativas, erros e limites do exemplo

As propriedades opcionais habilitam três tentativas de consumo. Depois de esgotadas,
a mensagem é rejeitada sem requeue infinito e encaminhada à fila de erros configurada
na fila principal. Não capture a exceção do service fingindo sucesso no consumer.
Corrija a causa e analise a mensagem antes de reenviar; ela não é salva automaticamente
só por estar na fila de erros.

Este exemplo não implementa outbox, publisher confirms ou idempotência. A publicação
sem erro imediato não equivale a confirmação de persistência pelo broker em todos os
cenários de falha. Também pode haver reentrega se o banco confirmar a gravação e a
conexão cair antes do ACK; repetir a mensagem pode criar outra avaliação.
Se o enunciado exigir garantia de entrega ou evitar duplicidade, adicione confirmações
de publicação e um identificador único da solicitação com tratamento idempotente.

Para desativar, reverta os passos: restaure o construtor/POST síncronos, comente as quatro
classes, as propriedades e a dependência. O service original continua funcionando.

Referências: [Listeners Spring AMQP](https://docs.spring.io/spring-amqp/reference/amqp/receiving-messages/async-annotation-driven.html)
e [conversor JSON](https://docs.spring.io/spring-amqp/reference/amqp/message-converters.html).
