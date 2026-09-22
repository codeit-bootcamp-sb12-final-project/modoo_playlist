package com.codeit.modoo_playlist.modulerealtime.security;

import java.security.Principal;
import java.util.UUID;

public record RealtimePrincipal(
        UUID userId,
        String email
) implements Principal {

    @Override
    public String getName() {
        return email;
    }
}
