package com.backend.gym_backend.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CookieUtil {

    public static void createJwtCookie(HttpServletResponse response, String token, String refreshToken) {

        Cookie cookie = new Cookie("jwt", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // true in production HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(15 * 60); //15 mins
//        cookie.setAttribute("SameSite", "None"); use in production

        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/owner/refresh");
        refreshCookie.setSecure(false); //true in production HTTPS
        refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
//        refreshCookie.setAttribute("SameSite", "None"); use in production

        response.addCookie(cookie);
        response.addCookie(refreshCookie);
    }

    public static void deleteJwtCookie(HttpServletResponse response) {

        Cookie cookie = new Cookie("jwt", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);

        response.addCookie(cookie);
    }

    public static String extractCookie(HttpServletRequest request, String name) {

        if(request.getCookies() == null) return null;

        for(Cookie cookie : request.getCookies()) {

            if(cookie.getName().equals(name)) {
                return cookie.getValue();
            }
        }

        return null;
    }

    public static void clearCookies(HttpServletResponse response){

        Cookie jwt = new Cookie("jwt", null);
        jwt.setPath("/");
        jwt.setHttpOnly(true);
        jwt.setMaxAge(0);
        jwt.setSecure(false);

        Cookie refresh = new Cookie("refreshToken", null);
        refresh.setPath("/auth/refresh");
        refresh.setMaxAge(0);
        refresh.setHttpOnly(true);
        refresh.setSecure(false);

        response.addCookie(jwt);
        response.addCookie(refresh);
    }
}
