package br.com.academia.auth;

import br.com.academia.shared.config.AcademiaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria o primeiro administrador a partir de variáveis de ambiente, somente se a tabela de usuários
 * estiver vazia. Assim nenhuma senha padrão fica gravada em migração ou no código.
 */
@Component
class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final AcademiaProperties props;

    AdminBootstrap(UsuarioRepository usuarios, PasswordEncoder encoder, AcademiaProperties props) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.props = props;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarios.count() > 0) {
            return;
        }
        var admin = props.admin();
        if (admin == null || isBlank(admin.username()) || isBlank(admin.password())) {
            log.warn("Nenhum usuário cadastrado e ACADEMIA_ADMIN_USERNAME/ACADEMIA_ADMIN_PASSWORD não definidos: "
                    + "não será possível fazer login.");
            return;
        }
        if (admin.password().length() < 8) {
            throw new IllegalStateException("A senha inicial do administrador deve ter pelo menos 8 caracteres");
        }
        String nome = isBlank(admin.nome()) ? "Administrador" : admin.nome();
        usuarios.save(new Usuario(admin.username(), encoder.encode(admin.password()), nome, Papel.ADMIN));
        log.info("Usuário administrador '{}' criado.", admin.username());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
