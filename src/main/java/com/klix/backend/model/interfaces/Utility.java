package com.klix.backend.model.interfaces;

import javax.servlet.http.HttpServletRequest;

public interface Utility {
    public static String getSiteURL(HttpServletRequest request) {
        String siteURL = request.getRequestURL().toString();
        return siteURL.replace(request.getServletPath(), "");
    }
}
