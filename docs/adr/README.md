# Architecture Decision Records

Decisões de arquitetura relevantes, no formato *contexto → decisão → consequências*.
Um ADR aceito não é editado: se a decisão mudar, cria-se um novo que o substitui.

| # | Decisão | Status |
|---|---|---|
| [0001](0001-monolito-modular.md) | Monólito modular por funcionalidade | Aceito |
| [0002](0002-postgresql-flyway.md) | PostgreSQL + Flyway; Hibernate só valida o schema | Aceito |
| [0003](0003-jwt-stateless.md) | JWT stateless emitido pela própria API | Aceito; limitações resolvidas pelo 0006 |
| [0004](0004-matricula-contador-atomico.md) | Matrícula com contador atômico por ano | Aceito |
| [0005](0005-estrategia-de-testes.md) | Testcontainers no backend, Playwright com API simulada no frontend | Aceito |
| [0006](0006-sessao-cookies-refresh-rotativo.md) | Sessão em cookies HttpOnly com refresh token rotativo | Aceito |
