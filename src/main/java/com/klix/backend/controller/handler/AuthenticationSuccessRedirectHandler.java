package com.klix.backend.controller.handler;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.klix.backend.constants.RoleConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;


/**
 * 
 */
public class AuthenticationSuccessRedirectHandler implements AuthenticationSuccessHandler
{
    protected final Log log = LogFactory.getLog(this.getClass());

    private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();


    public AuthenticationSuccessRedirectHandler(){
        super();
    }


    @Override
    public void onAuthenticationSuccess(final HttpServletRequest request, final HttpServletResponse response, final Authentication authentication)
        throws IOException
    {
        handle(request, response, authentication);
        clearAuthenticationAttributes(request);
    }


    /**
     * 
     */
    protected void handle(final HttpServletRequest request, final HttpServletResponse response, final Authentication authentication)
        throws IOException
    {
        final String targetUrl = determineTargetUrl(authentication);

        if (response.isCommitted()) {
            log.debug("Response has already been committed. Unable to redirect to " + targetUrl);
            return;
        }

        redirectStrategy.sendRedirect(request, response, targetUrl);
    }


    /**
     * 
     */
    protected String determineTargetUrl(final Authentication authentication)
    {
        Map<String, String> roleTargetUrlMap = new HashMap<>();
        roleTargetUrlMap.put(RoleConstants.user, "/user");
        roleTargetUrlMap.put(RoleConstants.admin, "/administration");
        roleTargetUrlMap.put(RoleConstants.developer, "/develop");

        final Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        // If user has no role, redirect to /
        if (authorities.isEmpty()) {
            log.debug("User has no authority");
            return "/";
        }

        for (final GrantedAuthority grantedAuthority : authorities)
        {
            String authorityName = grantedAuthority.getAuthority();
            if(roleTargetUrlMap.containsKey(authorityName))
            {
                return roleTargetUrlMap.get(authorityName);
            }
        }

        throw new IllegalStateException();
    }

    
    /**
     * Removes temporary authentication-related data which may have been stored in the session
     * during the authentication process.
     */
    protected final void clearAuthenticationAttributes(final HttpServletRequest request)
    {
        final HttpSession session = request.getSession(false);

        if (session == null) {
            return;
        }

        session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
    }
}