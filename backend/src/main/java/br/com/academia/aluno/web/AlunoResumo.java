package br.com.academia.aluno.web;

import br.com.academia.aluno.Aluno;
import br.com.academia.aluno.Situacao;

/** Linha da listagem/pesquisa: só o necessário para a tabela. */
public record AlunoResumo(String matricula, String nome, String rg, String email, String celular, Situacao situacao) {

    public static AlunoResumo of(Aluno a) {
        return new AlunoResumo(a.getMatricula(), a.getNome(), a.getRg(), a.getEmail(),
                formatarCelular(a.getCelular().ddd(), a.getCelular().numero()), a.getSituacao());
    }

    /** (91) 98888-7777 */
    static String formatarCelular(String ddd, String numero) {
        int corte = numero.length() - 4;
        return "(" + ddd + ") " + numero.substring(0, corte) + "-" + numero.substring(corte);
    }
}
