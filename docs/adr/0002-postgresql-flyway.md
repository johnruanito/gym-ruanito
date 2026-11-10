# 0002 — PostgreSQL + Flyway; Hibernate só valida o schema

**Status:** aceito

## Contexto
O legado usava `drop-and-create` no `persistence.xml`: cada deploy apagava o banco. Não havia
histórico de mudanças de schema nem restrições além das chaves.

## Decisão
- PostgreSQL 17 como banco único.
- Schema versionado em migrações SQL do Flyway (`db/migration`), executadas no start.
- `spring.jpa.hibernate.ddl-auto=validate`: o Hibernate confere o mapeamento contra o banco e falha no start se divergir.
- Regras de integridade também no banco: `CHECK` de formato (CPF, CEP), `UNIQUE`, FK `RESTRICT`, `saida >= entrada`.
- Busca por trecho de nome com índice trigram (`pg_trgm`).

## Consequências
- Mudanças de schema passam por revisão de código como qualquer outra mudança.
- O banco protege os dados mesmo contra acesso que não passe pela API.
- Dependência de recursos específicos do PostgreSQL (`pg_trgm`, `ON CONFLICT`). Aceitável: não há plano de trocar de banco.
