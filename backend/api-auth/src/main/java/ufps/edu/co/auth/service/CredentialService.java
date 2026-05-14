package ufps.edu.co.auth.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import ufps.edu.co.auth.contract.PasswordHashService;
import ufps.edu.co.auth.exception.InvalidCredentialsException;
import ufps.edu.co.auth.model.AuthPrincipal;
import ufps.edu.co.persistence.entities.UsuarioEntity;
import ufps.edu.co.persistence.repositories.UsuarioRepository;

@Service
public class CredentialService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordHashService passwordHashService;
    private final RoleNameNormalizer roleNameNormalizer;

    public CredentialService(
            UsuarioRepository usuarioRepository,
            PasswordHashService passwordHashService,
            RoleNameNormalizer roleNameNormalizer) {
        this.usuarioRepository = usuarioRepository;
        this.passwordHashService = passwordHashService;
        this.roleNameNormalizer = roleNameNormalizer;
    }

    public AuthPrincipal authenticate(String username, String rawPassword) {
        UsuarioEntity usuario = usuarioRepository.findByNombreusuario(username)
                .orElseThrow(InvalidCredentialsException::new);

        String storedPassword = Optional.ofNullable(usuario.getClave())
                .map(clave -> clave.getValor())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordHashService.matches(rawPassword, storedPassword)) {
            throw new InvalidCredentialsException();
        }

        return toPrincipal(usuario);
    }

    public AuthPrincipal toPrincipal(UsuarioEntity usuario) {
        List<String> roles = resolveRoles(usuario);
        return new AuthPrincipal(usuario.getId(), usuario.getNombreusuario(), roles);
    }

    public List<String> resolveRoles(UsuarioEntity usuario) {
        return Optional.ofNullable(usuario.getRol())
                .map(rol -> roleNameNormalizer.normalize(rol.getNombre()))
                .filter(role -> !role.isBlank())
                .map(List::of)
                .orElseGet(List::of);
    }
}