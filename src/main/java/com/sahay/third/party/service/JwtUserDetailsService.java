package com.sahay.third.party.service;


import com.sahay.third.party.model.Client;
import com.sahay.third.party.model.Role;
import com.sahay.third.party.repo.ClientRepository;
import com.sahay.third.party.repo.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
public class JwtUserDetailsService implements UserDetailsService {
    @Autowired
    private ClientRepository userDao;

    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private GlobalMethods globalMethods;

    @Autowired
    private PasswordEncoder bcryptEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Client user = userDao.findClientByUsername(username);

        if (globalMethods.isValid(username)) {
            user = userDao.findClientByUsername(username);
        }

        if (user == null) {
            throw new UsernameNotFoundException("User not found with username/email : " + username);
        }

        return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(),
                AuthorityUtils.createAuthorityList(user.getRole().getName()));
    }





}