import { useState, type FormEvent } from "react";
import { useTrocarMinhaSenha } from "../api/hooks";
import { useAuth } from "../auth/AuthContext";
import { Alert, Button, Card, Field, Input, PageHeader } from "../components/ui";

/** Qualquer usuário troca a própria senha. A senha atual é exigida: um computador destravado não basta. */
export function MinhaSenhaPage() {
  const trocar = useTrocarMinhaSenha();
  const { encerrarLocalmente } = useAuth();
  const [erroLocal, setErroLocal] = useState<string | null>(null);

  function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = e.currentTarget;
    const fd = new FormData(form);
    const senhaAtual = String(fd.get("senhaAtual"));
    const novaSenha = String(fd.get("novaSenha"));
    setErroLocal(null);
    if (novaSenha !== fd.get("confirmacao")) {
      setErroLocal("A confirmação não confere com a nova senha.");
      return;
    }
    // Trocar a senha encerra TODAS as sessões no servidor (inclusive esta): volta ao login
    trocar.mutate(
      { senhaAtual, novaSenha },
      {
        onSuccess: () => {
          form.reset();
          // Sem sessão, a rota protegida já redireciona para o login, que exibe o aviso
          encerrarLocalmente("Senha alterada. Entre novamente com a nova senha.");
        },
      },
    );
  }

  return (
    <div className="mx-auto max-w-md">
      <PageHeader
        title="Minha senha"
        subtitle="Mínimo de 8 caracteres, sem conter o seu usuário. Depois da troca, todas as suas sessões são encerradas."
      />
      <Card>
        <form onSubmit={onSubmit} className="space-y-4">
          {(erroLocal || trocar.error) && <Alert>{erroLocal ?? trocar.error?.message}</Alert>}
          <Field label="Senha atual">
            <Input name="senhaAtual" type="password" autoComplete="current-password" required />
          </Field>
          <Field label="Nova senha">
            <Input name="novaSenha" type="password" autoComplete="new-password" required minLength={8} />
          </Field>
          <Field label="Confirme a nova senha">
            <Input name="confirmacao" type="password" autoComplete="new-password" required minLength={8} />
          </Field>
          <Button type="submit" className="w-full" disabled={trocar.isPending}>
            Alterar senha
          </Button>
        </form>
      </Card>
    </div>
  );
}
