package com.merge.backend.global.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

@Getter
public class SecurityUser extends User {

    private Long id;
    private String name;

    public SecurityUser(String username, String password,
        Collection<? extends GrantedAuthority> authorities, Long id, String name) {
        super(username, password, authorities);
        this.id = id;
        this.name = name;
    }
}
