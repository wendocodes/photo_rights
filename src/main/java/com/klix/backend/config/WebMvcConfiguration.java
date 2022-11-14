package com.klix.backend.config;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

//@EnableWebMvc // Sollte gebraucht werden überschneidet sich aber vermutlich mit tymeleaf
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer
{
    @Bean("messageSource")
    public MessageSource messageSource() 
    {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("lang/messages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }

    /**
     * Ohne den SessionLocaleResolver wird mit dem AcceptHeaderLocaleResolver nach
     * dem Browserparameter "Accept-Language" und den vorhandenen lang-Definitionen entschieden.
     */
    @Bean
    public LocaleResolver localeResolver() 
    {
        AcceptHeaderLocaleResolver acceptHeaderLocaleResolver = new AcceptHeaderLocaleResolver();
        acceptHeaderLocaleResolver.setDefaultLocale(Locale.ENGLISH);
        return acceptHeaderLocaleResolver;
    }

    // für die parametrisierbare Spracheinstellung muss der vermutlich SessionLocaleResolver benutzt werden.
    // Ggf. funktioniert es mit dem richtigen Interceptor aber auch

    // @Bean
    // public LocaleChangeInterceptor localeChangeInterceptor() {
    //     LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
    //     localeChangeInterceptor.setParamName("lang");
    //     return localeChangeInterceptor;
    // }

    // @Override
    // public void addInterceptors(InterceptorRegistry registry) {
    //     registry.addInterceptor(localeChangeInterceptor());
    // }

    /**
     * Register MessageSource bean with the LocalValidatorFactoryBean to use localized messages on models
     */
    @Bean
    public LocalValidatorFactoryBean validator(MessageSource messageSource)
    {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource);
        return bean;
    }

    /**
     * Makes NetworkProperties injectable/autowireable
     */
    @Bean
    public NetworkProperties networkProperties()
    {
        return new NetworkProperties();
    }
}