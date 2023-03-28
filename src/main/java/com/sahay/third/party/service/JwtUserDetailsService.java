package com.sahay.third.party.service;


import com.sahay.third.party.model.Client;
import com.sahay.third.party.repo.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class JwtUserDetailsService implements UserDetailsService {
	@Autowired
	private ClientRepository userDao;
	@Autowired
	private GlobalMethods globalMethods;

	@Autowired
	private PasswordEncoder bcryptEncoder;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		Client user = userDao.findClientByUsername(username);
		if (globalMethods.isValid(username)){
			user = userDao.findClientByUsername(username);
		}

		if (user == null) {
			throw new UsernameNotFoundException("User not found with username/email : " + username);
		}
		return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(),
				new ArrayList<>());
	}


}