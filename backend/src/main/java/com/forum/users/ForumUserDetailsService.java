package com.forum.users;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Carga el usuario desde la base en cada autenticación. Como HTTP Basic autentica en cada
 * request, cualquier cambio de rol o estado aplica de inmediato.
 */
@Service
class ForumUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    ForumUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .map(AuthUser::from)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
    }
}
