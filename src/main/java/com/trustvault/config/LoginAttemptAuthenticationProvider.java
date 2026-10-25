
package com.trustvault.config;

import com.trustvault.service.LoginAttemptService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class LoginAttemptAuthenticationProvider
        implements AuthenticationProvider {

    private static final Logger logger =
            LoggerFactory.getLogger(
                    LoginAttemptAuthenticationProvider.class);

    private final DaoAuthenticationProvider delegate;
    private final LoginAttemptService loginAttemptService;

    public LoginAttemptAuthenticationProvider(
            DaoAuthenticationProvider delegate,
            LoginAttemptService loginAttemptService) {
        this.delegate = delegate;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {

        String username = authentication.getName();

        if (loginAttemptService.isBlocked(username)) {
            logger.warn("Login blocked for username: {}", username);

            throw new LockedException(
                    "Too many failed login attempts. Try again later.");
        }

        logger.info("Login attempt received for username: {}", username);

        try {
            Authentication result = delegate.authenticate(authentication);

            if (result == null) {
                throw new BadCredentialsException("Invalid credentials.");
            }

            loginAttemptService.loginSucceeded(username);

            logger.info("Authentication succeeded for username: {}",
                    username);

            return result;

        } catch (BadCredentialsException exception) {
            loginAttemptService.loginFailed(username);

            logger.warn(
                    "Failed login count for {}: {}",
                    username,
                    loginAttemptService.getFailedAttempts(username));

            throw exception;
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class
                .isAssignableFrom(authentication);
    }
}
