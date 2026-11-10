package com.ecom.service;

import com.ecom.dto.UserPrincipal;
import com.ecom.entity.UserEntity;
import com.ecom.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MyUserDetailsService implements UserDetailsService {
    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByEmail(username);

        if (user==null){
            System.out.println("User not found"+username);
            throw new UsernameNotFoundException("User not found");
        }

        return new UserPrincipal(user);
    }

}
