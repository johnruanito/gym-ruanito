# 0001 — Monólito modular por funcionalidade

**Status:** aceito

## Contexto
O sistema tem três conceitos centrais (aluno, acesso, usuário), um único time e um volume baixo
(algumas centenas de registros de acesso por dia em uma academia). O sistema legado misturava camadas
técnicas (`domain`, `application`, `interfaces`) e espalhava cada funcionalidade por vários pacotes.

## Decisão
Um único deployable Spring Boot, organizado **por funcionalidade** (`aluno`, `acesso`, `auth`, `relatorio`),
cada uma com seu domínio, serviço, repositório e controller. Dependências entre módulos só por classes
públicas de serviço; `acesso` depende de `aluno`, nunca o contrário.

## Consequências
- Um deploy, uma transação, um banco: sem consistência eventual nem rede entre serviços.
- Fronteiras explícitas permitem extrair um módulo (ex.: `acesso` para integração com catraca) se a escala exigir.
- Risco: as fronteiras são por convenção. Mitigação planejada: testes de arquitetura (Spring Modulith ou ArchUnit) no CI.
