package com.an.admit_one.service;

import com.an.admit_one.entity.User;
import com.an.admit_one.repository.AuthRepository;
import com.an.admit_one.security.UserPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomerUserDetailsService implements UserDetailsService {

    private final AuthRepository authRepository;

    public CustomerUserDetailsService(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user = authRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Email không tồn tại"));

        return new UserPrincipal(user);
    }
}