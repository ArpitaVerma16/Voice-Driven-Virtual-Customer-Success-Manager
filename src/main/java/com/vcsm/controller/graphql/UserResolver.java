package com.vcsm.controller.graphql;

import com.vcsm.model.User;
import com.vcsm.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class UserResolver {

    private final UserService userService;

    @QueryMapping
    public User me() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.getUserByEmail(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @QueryMapping
    public User user(@Argument Long id) {
        return userService.getUserById(id)
            .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }
}