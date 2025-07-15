package com.redmath.newsapp.user;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class CurrentUser {
    public static User get(){
        Authentication auth= SecurityContextHolder.getContext().getAuthentication();
        if(auth!=null && auth.getPrincipal() instanceof User user){
            return user;
        }
        return null;
    }
}
