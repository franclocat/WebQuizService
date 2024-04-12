
package com.example.WebQuizService;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

//uses the AppUser repository to find users and get the Detail of a traduced User object instead of the AppUser object
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public AppUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser appUser = appUserRepository
                .findAppUserByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Username not found"));

        return new AppUserAdapter(appUser);
    }
}