# 0006 — Sessão em cookies HttpOnly com refresh token rotativo

**Status:** aceito. Substitui as "limitações conhecidas" do [ADR 0003](0003-jwt-stateless.md).

## Contexto
O ADR 0003 registrou três limitações do JWT de 8h guardado em `sessionStorage`:
1. legível por JavaScript: uma falha de XSS entregaria a sessão;
2. sem revogação: desativar um funcionário ou suspender uma academia só valia quando o token vencesse;
3. sem proteção contra força bruta no login (tratado no ADR seguinte).

Com a multi-academia, o problema 2 ficou comercialmente relevante: suspender um cliente inadimplente
precisa valer em minutos, não em horas.

## Decisão

| Peça | Escolha | Motivo |
|---|---|---|
| Access token | JWT, **15 min**, cookie `access_token` (`Path=/api`) | Curto: se vazar, vale pouco. Sem estado no servidor para cada requisição |
| Refresh token | 256 bits aleatórios, cookie `refresh_token` (`Path=/api/auth`) | Opaco (não é JWT): só o servidor sabe o que significa |
| Armazenamento do refresh | Tabela `sessao`, **só o hash SHA-256** | Um dump do banco não permite usar os tokens |
| Rotação | Cada `/refresh` revoga o token usado e emite outro | Um token copiado deixa de valer assim que o legítimo renova |
| Detecção de reuso | Token já trocado reaparece → revoga a **família** inteira | Sinal de roubo: derruba atacante e vítima; a vítima só refaz login |
| Prazo da sessão | 12h absolutas desde o login; renovar não estende | Ninguém fica logado indefinidamente |
| Cookies | `HttpOnly; Secure; SameSite=Strict` | XSS não lê; só trafega em HTTPS; outro site não consegue usar (CSRF) |
| Revogação ativa | Desativar usuário, redefinir ou trocar senha revogam todas as sessões dele | O acesso acaba em no máximo 15 min (validade do access token já emitido) |

Clientes que não são navegador (testes, integrações, Swagger) continuam podendo enviar o access token
em `Authorization: Bearer`.

### Por que não CSRF token?
A SPA e a API estão no mesmo domínio (nginx), e os cookies usam `SameSite=Strict`: o navegador não os
envia em requisições originadas de outro site. Além disso, a API só aceita `application/json`, que um
formulário HTML de outro site não consegue enviar sem uma requisição preflight de CORS, que a API não libera.

### Por que tolerância de reuso (10 s)?
Duas abas podem renovar ao mesmo tempo com o mesmo cookie. Sem tolerância, a segunda seria tratada como
roubo e derrubaria o usuário. Na janela de 10 s o servidor emite outro token da mesma família; depois
dela, reuso é tratado como roubo. No frontend, renovações simultâneas na mesma aba são unificadas
("single-flight").

## Consequências
- Logout, desativação e troca de senha passam a ter efeito real no servidor.
- O banco ganha uma escrita por renovação (a cada ~15 min por usuário ativo), irrelevante no volume de uma academia.
- `Secure` exige HTTPS. `localhost` é tratado como seguro pelos navegadores; para acessar por IP da rede
  local sem TLS, é preciso `ACADEMIA_COOKIE_SEGURO=false` (nunca em produção).
- Sessões expiradas há mais de 7 dias são apagadas por um job diário.
