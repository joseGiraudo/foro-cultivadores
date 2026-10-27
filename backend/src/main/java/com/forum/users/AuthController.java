package com.forum.users;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.forum.shared.error.ApiException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
class AuthController {

    private final UserService userService;
    private final AccountService accountService;
    private final AuthenticationManager authenticationManager;
    // Persiste el SecurityContext en la sesión HTTP (cookie de sesión), reemplazo del HTTP Basic.
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    AuthController(UserService userService, AccountService accountService,
                   AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.accountService = accountService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    /**
     * Valida usuario/contraseña y abre sesión (cookie httpOnly). Los suspendidos igual pueden iniciar
     * sesión (pueden leer); la escritura se bloquea después en la capa de servicio.
     */
    @PostMapping("/login")
    UserResponse login(@Valid @RequestBody LoginRequest request,
                       HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        } catch (AuthenticationException e) {
            throw ApiException.unauthorized("Usuario o contraseña incorrectos");
        }

        AuthUser principal = (AuthUser) authentication.getPrincipal();
        // Gate de verificación: sin email verificado no se abre sesión (no puede ingresar).
        if (!principal.isEmailVerified()) {
            throw ApiException.forbidden("Verificá tu email antes de ingresar. Revisá tu casilla.");
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        return userService.getMe(principal);
    }

    /** Cierra la sesión (invalida la cookie de sesión). Idempotente. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    /** Verifica el email a partir del token del link. 400 si el enlace es inválido o venció. */
    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        accountService.verifyEmail(request.token());
    }

    /** Reenvía el email de verificación. Respuesta genérica (no revela si el email existe). */
    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void resendVerification(@Valid @RequestBody EmailOnlyRequest request) {
        accountService.resendVerification(request.email());
    }

    /** Inicia la recuperación de contraseña. Respuesta genérica (no revela si el email existe). */
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void forgotPassword(@Valid @RequestBody EmailOnlyRequest request) {
        accountService.forgotPassword(request.email());
    }

    /** Cambia la contraseña a partir del token de reset. 400 si el enlace es inválido o venció. */
    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        accountService.resetPassword(request.token(), request.password());
    }
}
