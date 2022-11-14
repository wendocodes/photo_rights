package com.klix.backend.service.user;

import com.klix.backend.model.Role;
import com.klix.backend.model.User;
import com.klix.backend.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;


/**
 * 
 */
@Service
public class UserDetailsServiceImp implements UserDetailsService
{
    @Autowired
    private UserRepository userRepository;


    /**
     * 
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username)
    {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null)
        {
            throw new UsernameNotFoundException(username);
        }

        Set<GrantedAuthority> grantedAuthorities = new HashSet<>();
        for (Role role : user.getRoles()) grantedAuthorities.add(new SimpleGrantedAuthority(role.getName()));

        return new org.springframework.security.core.userdetails.User(user.getUsername(),
                                                                      user.getPassword(),
                                                                      grantedAuthorities);
    }
}