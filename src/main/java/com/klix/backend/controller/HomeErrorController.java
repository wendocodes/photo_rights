package com.klix.backend.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;


/**
 * 
 */
@Slf4j
@Controller
public class HomeErrorController implements ErrorController
{
    /**
     * 
     */
    @Override
    public String getErrorPath() {
        return "error";
    }
    

    /**
     * 
     */
    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model)
    {
        Exception exeption = (Exception)request.getAttribute("javax.servlet.error.exception");
        String exceptionMessage = "error";

        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        String remoteIP = request.getRemoteHost();      

        if (exeption != null)
        {
            exceptionMessage = exeption.getMessage();
            log.error("HomeErrorController /error was called, exception.message: " + exceptionMessage
                        + ", status: " + statusCode
                        + ", remote IP: " + remoteIP);
        }
        else
        {
            log.warn("HomeErrorController /error was called, "
                        + ", status: " + statusCode
                        + ", remote IP: " + remoteIP);            
        }

        model.addAttribute("exceptionMessage", exceptionMessage);
        model.addAttribute("statusCode", statusCode);
        model.addAttribute("remoteIP", remoteIP);
        return "errors/error";
    }
 

    /**
     * 
     */
    @GetMapping("/access-denied")
    public String accessDenied() {
        log.info("HomeErrorController /access-denied was called");
        return "errors/access-denied";
    }
}