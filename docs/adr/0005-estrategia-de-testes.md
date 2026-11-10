# 0005 — Estratégia de testes

**Status:** aceito

## Contexto
O legado não tinha testes. Queremos confiança para refatorar sem depender de teste manual,
mantendo o CI rápido.

## Decisão

| Nível | Ferramenta | O que cobre |
|---|---|---|
| Unitário (backend) | JUnit 5 + AssertJ | Regras de domínio puras: CPF, matrícula, entrada/saída/abandono |
| Integração (backend) | Spring Boot Test + **Testcontainers (PostgreSQL real)** + MockMvc | Contrato HTTP, segurança, migrações, SQL específico do PostgreSQL, concorrência otimista |
| Unitário (frontend) | Vitest | Validação de CPF e formatadores |
| E2E (frontend) | **Playwright** com API simulada (`page.route`) | Fluxos de tela: login, redirecionamento, cadastro, erros por campo, catraca, filtros na URL |

- Testcontainers em vez de H2: o schema usa recursos do PostgreSQL (`pg_trgm`, `ON CONFLICT`, `TIMESTAMPTZ`) que um banco em memória não reproduz.
- E2E com API simulada: testes determinísticos e rápidos, sem subir banco. O contrato real fica coberto pelo teste de integração do backend.
- Cobertura do backend (JaCoCo, unitários + integração) publicada no resumo de cada execução do CI.

## Consequências
- `mvn verify` precisa de Docker (Testcontainers).
- O mock do E2E pode divergir da API real. Mitigação futura: gerar os tipos do frontend a partir do OpenAPI do backend.
