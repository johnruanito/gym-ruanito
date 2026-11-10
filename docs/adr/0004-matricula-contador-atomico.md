# 0004 — Matrícula com contador atômico por ano

**Status:** aceito

## Contexto
A matrícula tem o formato `AAAA` + sequencial de 4 dígitos (ex.: `20260042`) e é a chave usada pela
recepção e impressa em carteirinhas. O legado gerava com `SELECT MAX(matricula) + 1`: dois cadastros
simultâneos liam o mesmo máximo e o segundo falhava com chave duplicada.

## Decisão
Tabela `matricula_sequencia(ano, ultimo)` incrementada com um único comando atômico:

```sql
INSERT INTO matricula_sequencia (ano, ultimo) VALUES (:ano, 1)
ON CONFLICT (ano) DO UPDATE SET ultimo = matricula_sequencia.ultimo + 1
RETURNING ultimo
```

executado na mesma transação do cadastro (`Propagation.MANDATORY`).

## Consequências
- Sem colisão de chave em cadastros concorrentes; o reinício anual do sequencial é natural.
- Sem lacunas na numeração: se o cadastro falhar, o incremento sofre rollback junto.
- Cadastros do mesmo ano são serializados no lock da linha do contador. Irrelevante no volume de uma academia.
- Alternativa descartada: `SEQUENCE` do PostgreSQL (não reinicia por ano e gera lacunas em rollback).
