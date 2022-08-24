package io.ml.proxy.server.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UsernamePasswordAuth {
    private String username;
    private String password;

    public String getUsernameAndPassword() {
        return username + ":" + password;
    }
}
