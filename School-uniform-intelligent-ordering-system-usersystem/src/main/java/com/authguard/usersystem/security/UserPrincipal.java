package com.authguard.usersystem.security;

import java.security.Principal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPrincipal implements Principal {
    private Long userId;
    private String userAccount;

    @Override
    public String getName() {
        return userAccount;
    }
}
