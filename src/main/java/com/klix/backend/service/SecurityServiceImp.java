package com.klix.backend.service;

import com.klix.backend.service.interfaces.SecurityService;
import com.klix.backend.service.user.UserService;
import com.klix.backend.constants.RoleConstants;
import com.klix.backend.model.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 */
@Slf4j
@Service
public class SecurityServiceImp implements SecurityService
{
    @Autowired private AuthenticationManager authenticationManager;

    @Autowired private UserDetailsService userDetailsService;
    @Autowired private UserService userService;


    /**
     * 
     */
    @Override
    public String findLoggedInUsername() {
        Object userDetails = SecurityContextHolder.getContext().getAuthentication().getDetails();
        if (userDetails instanceof UserDetails) {
            return ((UserDetails) userDetails).getUsername();
        }

        return null;
    }


    /**
     * Die Funktion ermittelt den aktuellen User.
     */
    @Override
    public User findLoggedInUser()
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();
        return userService.findByUsername(currentPrincipalName);
    }



    /**
     * Die Funktion ermittelt ob der aktuelle User ein Admin ist.
     */
    public boolean isUserAdmin()
    {
        User user = findLoggedInUser();
        return user.getRoles().stream().anyMatch(r -> r.getName().equals(RoleConstants.admin));
    }


    /**
     * Noch ungetestet
     */
    @Override
    public void autoLogin(String username, String password) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                userDetails, password, userDetails.getAuthorities());

        authenticationManager.authenticate(token);

        if (token.isAuthenticated()) {
            SecurityContextHolder.getContext().setAuthentication(token);
            log.debug(String.format("Auto login %s successful!", username));
        }
    }
}