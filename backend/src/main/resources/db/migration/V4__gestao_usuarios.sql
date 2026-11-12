-- Gestão de usuários: novo papel INSTRUTOR e data de criação do usuário.

-- O CHECK original (V1) só aceitava ADMIN e RECEPCAO.
ALTER TABLE usuario DROP CONSTRAINT usuario_papel_check;
ALTER TABLE usuario ADD CONSTRAINT usuario_papel_check
    CHECK (papel IN ('ADMIN', 'RECEPCAO', 'INSTRUTOR'));

ALTER TABLE usuario ADD COLUMN criado_em TIMESTAMPTZ NOT NULL DEFAULT now();

-- A tela "Equipe" lista os usuários de UMA academia: índice pela coluna do filtro.
CREATE INDEX usuario_academia_idx ON usuario (academia_id);
