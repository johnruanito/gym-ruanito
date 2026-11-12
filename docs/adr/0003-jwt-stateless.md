# 0003 — JWT stateless emitido pela própria API

**Status:** aceito. Limitações de armazenamento e revogação resolvidas pelo [ADR 0006](0006-sessao-cookies-refresh-rotativo.md)

## Contexto
O legado tinha uma tela de login sem nenhum mecanismo de autenticação por trás. Precisamos de
autenticação e de papéis (ADMIN, RECEPCAO) para uma SPA servida no mesmo domínio da API.

## Decisão
- `POST /api/auth/login` valida a senha (BCrypt via `DelegatingPasswordEncoder`) e emite um JWT HS256 com validade de um turno (8h).
- A API é um *resource server* stateless (Spring Security `oauth2ResourceServer().jwt()`); os papéis vêm do claim `roles`.
- O admin inicial é criado a partir de variáveis de ambiente, nunca de uma senha fixa em migração.
- Verificação de senha em tempo constante mesmo para usuário inexistente (hash fictício), evitando enumeração de usuários.

## Consequências
- Sem estado de sessão no servidor: escala horizontal trivial.
- **Limitações conhecidas** (aceitas na v2.0):
  - o token fica em `sessionStorage`, legível por JavaScript em caso de XSS;
  - não há revogação antes da expiração (logout é só no cliente);
  - não há limite de tentativas de login.
- **Revisão planejada:** cookie `HttpOnly; Secure; SameSite=Strict` com access token curto + refresh token
  rotativo persistido (permite revogação), e rate limiting no login.
