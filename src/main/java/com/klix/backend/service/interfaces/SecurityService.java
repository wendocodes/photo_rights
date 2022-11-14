package com.klix.backend.service.interfaces;

import com.klix.backend.model.User;

/**
 * 
 */
public interface SecurityService
{
    String findLoggedInUsername();

    /**
     * @return  the logged in user or null if not logged in
     */
    User findLoggedInUser();

    boolean isUserAdmin();

    void autoLogin(String username, String password);
}