# Sistema de avaliações

API Java 25 / Spring Boot com PostgreSQL e autenticação por access token JWT do Auth0.
O backend está configurado para o tenant `dev-xmwdlx52eoqalffj.us.auth0.com` e audience
`https://prova-api`. A API, os papéis e a Action no painel precisam corresponder aos valores abaixo.
Os testes usam H2 e um decodificador simulado, sem exigir PostgreSQL nem conta Auth0.

## Contrato

| Método | Rota | Papel | Resposta |
| --- | --- | --- | --- |
| POST | `/avaliacoes` | ADMIN | 201 com a avaliação cadastrada |
| GET | `/avaliacoes` | ADMIN ou USER | 200 com todas as avaliações, mais recentes primeiro |
| GET | `/avaliacoes/{id}` | ADMIN ou USER | 200 com a avaliação; 404 se não existir |
| DELETE | `/avaliacoes/{id}` | ADMIN | 204 sem corpo; 404 se não existir |
| PATCH | `/avaliacoes/{id}/status` | ADMIN | 200 com a avaliação atualizada; 404 se não existir |

Todas as chamadas exigem `Authorization: Bearer <access_token>`.
Sem token ou com token inválido: 401. Sem papel autorizado: 403.
Dados inválidos: 400 com os erros por campo. A lista vazia retorna `[]`.

Corpo do POST:

```json
{
  "emailAvaliado": "colega@empresa.com",
  "comentarios": "Boa colaboração com a equipe.",
  "nota": 8.5
}
```

`emailAvaliado` deve ser um e-mail válido de até 255 caracteres; `comentarios` é obrigatório
e aceita até 4000 caracteres. A nota é obrigatória, entre 0 e 10 inclusive, com até duas casas decimais.
O servidor gera `id` e `dataAvaliacao` (UTC). O `emailAvaliador` vem do token autenticado:
não deve ser enviado no corpo. Por isso o POST exige um e-mail válido no token.

Exemplo de resposta:

```json
{
  "id": 1,
  "emailAvaliador": "admin@empresa.com",
  "emailAvaliado": "colega@empresa.com",
  "comentarios": "Boa colaboração com a equipe.",
  "nota": 8.5,
  "dataAvaliacao": "2026-11-10T21:00:00Z"
}
```

## Status da avaliação

O enum `StatusAvaliacao` aceita `PENDENTE` e `VISTO`. Novas avaliações começam como
`PENDENTE`. O status aparece no JSON e fica salvo pelo nome no banco (`EnumType.STRING`).
Com `ddl-auto=update`, ao reiniciar a aplicação, a coluna será criada com default
`PENDENTE` também para registros antigos.

Somente ADMIN pode alterar o status, usando `PATCH /avaliacoes/{id}/status`:

```json
{ "status": "VISTO" }
```

Também é possível voltar para `PENDENTE`. Status ausente, nulo ou desconhecido retorna 400.
USER recebe 403 e requisições sem token recebem 401. A restrição está no `SecurityConfig`:

```java
.requestMatchers(HttpMethod.PATCH, "/avaliacoes/{id}/status").hasRole("ADMIN")
```

O `requestMatchers` seleciona a rota e o método HTTP; o `hasRole` define o papel exigido.
Essa regra fica antes de `anyRequest().denyAll()`. Ao ativar soft delete, a atualização
também respeita o filtro de `buscarPorId` e retorna 404 para avaliações excluídas.

## Alternativa de soft delete (comentada no código)

O hard delete continua ativo. Para trocar para soft delete, siga os comentários `SOFT DELETE`:

1. Em `Avaliacao.java`, descomente o campo `excluida`, suas anotações e o método
   `marcarComoExcluida()`.
2. Em `AvaliacaoRepository.java`, descomente os dois métodos com `ExcluidaFalse`.
3. Em `AvaliacaoService.buscarPorId`, substitua `repository.findById(id)` por
   `repository.findByIdAndExcluidaFalse(id)`, mantendo o `orElseThrow`.
4. Em `AvaliacaoService.excluir`, comente a linha `repository.delete(...)` e descomente
   as três linhas da alternativa de soft delete.
5. Em `AvaliacaoService.listar`, comente o return atual e descomente o return com
   `findAllByExcluidaFalse`.

Reinicie a aplicação. Com `spring.jpa.hibernate.ddl-auto=update`, o Hibernate adiciona
a coluna `excluida` ao PostgreSQL; a definição `boolean default false` permite manter
os registros anteriores como ativos. Se o banco for gerenciado por migrações ou o usuário
da aplicação não puder alterar a tabela, adicione a coluna por uma migração:

```sql
ALTER TABLE avaliacoes ADD COLUMN excluida boolean NOT NULL DEFAULT false;
```

O controller, as rotas, as permissões e os códigos HTTP não precisam mudar.
A avaliação permanece no banco, mas GET por ID e listagem deixam de exibi-la.
Uma segunda exclusão do mesmo ID retorna 404.

Atualize também o teste `adminExclui`: substitua a expectativa de registro ausente por
`assertThat(repository.findById(id)).hasValueSatisfying(avaliacao -> assertThat(avaliacao.isExcluida()).isTrue());`.
Mantenha a verificação de 404 no GET e acrescente uma consulta à listagem para confirmar
que a avaliação excluída não aparece. Os métodos herdados `findById` e `findAll` continuam
enxergando os registros excluídos; nas consultas da API, use os métodos com `ExcluidaFalse`.

## Configurar o Auth0

1. Crie uma API no Auth0, com Identifier `https://prova-api` e assinatura RS256.
2. Crie uma aplicação do tipo Single Page Application para o front. Configure as URLs
   de callback, logout e origem conforme a URL do front (por exemplo, `http://localhost:5173`).
3. Crie os papéis `ADMIN` e `USER` e atribua-os aos usuários de teste.
4. Crie uma Action de Login / Post Login com o código abaixo, faça o deploy da Action
   e adicione-a ao fluxo de Login.

```javascript
exports.onExecutePostLogin = async (event, api) => {
  const namespace = 'https://prova-api';
  if (event.resource_server?.identifier !== namespace) return;

  api.accessToken.setCustomClaim(
    `${namespace}/roles`, event.authorization?.roles || []
  );
  if (event.user.email) {
    api.accessToken.setCustomClaim(`${namespace}/email`, event.user.email);
  }
};
```

5. Defina `AUTH0_ISSUER_URI` no backend com o domínio do seu tenant, incluindo `https://`
   e a barra final. Defina `AUTH0_AUDIENCE` com o Identifier da API.
6. No login do front, solicite essa mesma audience e os scopes `openid profile email`.
   Envie o **access token** nas chamadas à API. Faça login novamente após alterar papéis ou a Action.

Os nomes dos claims são configuráveis por `AUTH0_ROLES_CLAIM` e `AUTH0_EMAIL_CLAIM`.
Por padrão, os papéis devem estar em `https://prova-api/roles` como array
(`['ADMIN']` ou `['USER']`), e o e-mail em `https://prova-api/email`.
Também há suporte ao claim `email` como alternativa. Os nomes dos papéis distinguem maiúsculas de minúsculas.
A API verifica assinatura, validade, issuer e audience do JWT.

Referências: [JWT no Spring Security](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
e [papéis em tokens por Auth0 Actions](https://auth0.com/docs/manage-users/access-control/sample-use-cases-actions-with-authorization).

## Executar

Use JDK 25. No PowerShell, na pasta deste projeto:

```powershell
$env:DB_HOST = 'localhost'
$env:DB_USER = 'usuario'
$env:DB_PASSWORD = 'senha'
$env:AUTH0_ISSUER_URI = 'https://SEU-TENANT.us.auth0.com/'
$env:AUTH0_AUDIENCE = 'https://prova-api'
.\mvnw.cmd spring-boot:run
```

O banco `auladb` deve existir no PostgreSQL, porta 5432. O Hibernate cria/atualiza a tabela
`avaliacoes` com a configuração já existente do projeto. A API atende na porta 8080.
Para outra origem do front, defina `CORS_ALLOWED_ORIGINS` (origens separadas por vírgulas).
No deploy, os valores Auth0 padrão já correspondem ao tenant e à audience deste projeto.
O workflow passa `DB_HOST` e a Variable do GitHub `CORS_ALLOWED_ORIGINS` ao contêiner.
Defina essa Variable com as origens local e da Vercel, separadas por vírgula e sem barra final.
Se precisar sobrescrever os valores Auth0 no servidor, passe as variáveis correspondentes
ao contêiner. O README do front detalha a configuração conjunta e os dados pendentes.

Exemplo no PowerShell após obter um access token pelo login:

```powershell
$token = 'SEU_ACCESS_TOKEN'
$headers = @{ Authorization = "Bearer $token" }
$body = @{ emailAvaliado = 'colega@empresa.com'; comentarios = 'Boa colaboração'; nota = 8.5 } | ConvertTo-Json
Invoke-RestMethod -Uri 'http://localhost:8080/avaliacoes' -Method Post -Headers $headers -ContentType 'application/json' -Body $body
Invoke-RestMethod -Uri 'http://localhost:8080/avaliacoes' -Headers $headers
```

## Testar e estudar

```powershell
.\mvnw.cmd test
```

Os testes cobrem permissões, tokens ausentes/inválidos, cadastro e leitura no banco,
campos obrigatórios, e-mails, nota e seus limites. A validação criptográfica real do Auth0
fica a cargo do Spring Security; o teste simula a decodificação para verificar as regras da API.

Fluxo para estudar: `SecurityConfig` valida o acesso → `AvaliacaoController` recebe e valida
o JSON → `AvaliacaoService` executa o cadastro/listagem → `AvaliacaoRepository` acessa o banco.
`Avaliacao` representa a tabela; `CriarAvaliacaoRequest` define os dados aceitos no POST.
