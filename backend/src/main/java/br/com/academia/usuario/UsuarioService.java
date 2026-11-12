package br.com.academia.usuario;

import br.com.academia.auth.Papel;
import br.com.academia.auth.Usuario;
import br.com.academia.auth.UsuarioRepository;
import br.com.academia.auth.sessao.SessaoService;
import br.com.academia.shared.error.ConflitoException;
import br.com.academia.shared.error.RecursoNaoEncontradoException;
import br.com.academia.shared.error.RegraNegocioException;
import br.com.academia.shared.tenant.TenantContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Gestão da equipe de UMA academia (a do usuário logado).
 * <p>
 * Como {@link Usuario} não tem filtro automático de tenant, TODO método começa obtendo a academia
 * via {@link TenantContext#exigir()} e a usa na consulta. Um id de usuário de outra academia
 * simplesmente "não existe" (404), sem revelar que existe em outro lugar.
 */
@Service
@Transactional(readOnly = true)
public class UsuarioService {

    public record NovoUsuario(String username, String nome, Papel papel, String senha) {
    }

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final SessaoService sessoes;

    UsuarioService(UsuarioRepository usuarios, PasswordEncoder encoder, SessaoService sessoes) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.sessoes = sessoes;
    }

    public List<Usuario> listar() {
        return usuarios.findByAcademiaIdOrderByNomeAsc(TenantContext.exigir());
    }

    public Usuario buscar(Long id) {
        return usuarios.findByIdAndAcademiaId(id, TenantContext.exigir())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
    }

    @Transactional
    public Usuario criar(NovoUsuario dados) {
        String username = dados.username().strip().toLowerCase(Locale.ROOT);
        // Username é único na PLATAFORMA (o login não pergunta a academia), não só na academia.
        if (usuarios.existsByUsernameIgnoreCase(username)) {
            throw new ConflitoException("Nome de usuário já está em uso");
        }
        SenhaPolicy.validar(dados.senha(), username);
        var usuario = new Usuario(TenantContext.exigir(), username, encoder.encode(dados.senha()),
                dados.nome().strip(), dados.papel());
        return usuarios.save(usuario);
    }

    @Transactional
    public Usuario atualizar(Long id, String nome, Papel papel) {
        Usuario usuario = buscar(id);
        if (usuario.getPapel() == Papel.ADMIN && papel != Papel.ADMIN) {
            garantirOutroAdminAtivo(usuario, "rebaixar");
        }
        usuario.atualizar(nome.strip(), papel);
        return usuario;
    }

    @Transactional
    public Usuario desativar(Long id, String usernameDoAtor) {
        Usuario usuario = buscar(id);
        if (usuario.getUsername().equalsIgnoreCase(usernameDoAtor)) {
            throw new RegraNegocioException("Você não pode desativar o próprio usuário");
        }
        if (usuario.getPapel() == Papel.ADMIN) {
            garantirOutroAdminAtivo(usuario, "desativar");
        }
        usuario.desativar();
        // Derruba as sessões: a próxima renovação falha e o acesso acaba em no máximo 15 min (access token)
        sessoes.revogarTodas(usuario.getId());
        return usuario;
    }

    @Transactional
    public Usuario reativar(Long id) {
        Usuario usuario = buscar(id);
        usuario.reativar();
        return usuario;
    }

    /** O dono define uma senha provisória para alguém que esqueceu a sua. */
    @Transactional
    public void redefinirSenha(Long id, String novaSenha) {
        Usuario usuario = buscar(id);
        SenhaPolicy.validar(novaSenha, usuario.getUsername());
        usuario.trocarSenha(encoder.encode(novaSenha));
        sessoes.revogarTodas(usuario.getId()); // quem estava logado com a senha antiga é desconectado
    }

    /** Qualquer usuário troca a própria senha, confirmando a atual. */
    @Transactional
    public void trocarMinhaSenha(String username, String senhaAtual, String novaSenha) {
        Usuario usuario = usuarios.findByUsernameIgnoreCaseAndAcademiaId(username, TenantContext.exigir())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
        if (!encoder.matches(senhaAtual, usuario.getSenhaHash())) {
            throw new RegraNegocioException("Senha atual incorreta");
        }
        if (senhaAtual.equals(novaSenha)) {
            throw new RegraNegocioException("A nova senha deve ser diferente da atual");
        }
        SenhaPolicy.validar(novaSenha, username);
        usuario.trocarSenha(encoder.encode(novaSenha));
        // Senha trocada = possível suspeita de vazamento: encerra TODAS as sessões, inclusive esta
        sessoes.revogarTodas(usuario.getId());
    }

    /** Impede que a academia fique sem nenhum administrador ativo (ninguém mais conseguiria gerir a equipe). */
    private void garantirOutroAdminAtivo(Usuario alvo, String acao) {
        long adminsAtivos = usuarios.countByAcademiaIdAndPapelAndAtivoTrue(alvo.getAcademiaId(), Papel.ADMIN);
        long restantes = alvo.isAtivo() ? adminsAtivos - 1 : adminsAtivos;
        if (restantes < 1) {
            throw new RegraNegocioException(
                    "Não é possível " + acao + " o último administrador ativo da academia");
        }
    }
}
