package com.klix.backend.config;

import com.klix.backend.constants.RoleConstants;
import com.klix.backend.controller.handler.LoggingAccessDeniedHandler;
import com.klix.backend.controller.handler.AuthenticationSuccessRedirectHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter
{
    @Qualifier("userDetailsServiceImp")
    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth)
    throws Exception
    {
        auth.userDetailsService(userDetailsService).passwordEncoder(bCryptPasswordEncoder());
    }

    @Autowired
    private LoggingAccessDeniedHandler accessDeniedHandler;

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() 
    {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public AuthenticationManager customAuthenticationManager()
    throws Exception 
    {
        return authenticationManager();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessRedirectHandler()
    {
        return new AuthenticationSuccessRedirectHandler();
    }


    /**
     *
     */
    @Override
    protected void configure(HttpSecurity http)
    throws Exception
    {   
        http.cors().and()
            .csrf().disable() // Das muss später weg!
                .authorizeRequests()
                .antMatchers("/",
                             "/js/**",
                             "/css/**",
                             "/img/**",
                             "/webjars/**",
                             "/error/**",
                             "/access-denied",
                             "/registration",
                             "/verify_pin",
                             "/user/create_credentials",
                             "/create_credentials",
                             "/forgot_password",
                             "/reset_password",
                             "/favicon.ico").permitAll()

                .antMatchers("/administration/**")
                    .hasAnyAuthority(RoleConstants.admin, RoleConstants.developer)
                .antMatchers("/user")
                    .hasAnyAuthority(RoleConstants.user, RoleConstants.admin, RoleConstants.developer)
                .antMatchers("/user/**")
                    .hasAnyAuthority(RoleConstants.user)
                .antMatchers("/develop/**")
                    .hasAuthority(RoleConstants.developer)
                .antMatchers("/client/**")
                    .hasAnyAuthority(RoleConstants.user)

                // Test API (Für Rechte, zum Testen)
                .antMatchers(HttpMethod.POST, "/api/**")
                    .hasAuthority(RoleConstants.developer)

                // APP-API
                .antMatchers(HttpMethod.POST, "/app/api/v01/**")
                    .permitAll()
                .antMatchers(HttpMethod.GET, "/app/api/v01/**")
                    .permitAll()

                // Allow CORS option calls:
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll() 
                    .anyRequest().authenticated()
                .and()
                .formLogin()
                    .loginPage("/login")
                    .defaultSuccessUrl("/index")
                    .permitAll()
                    .successHandler(authenticationSuccessRedirectHandler())

                .and()
                .logout()
                    .invalidateHttpSession(true)
                    .clearAuthentication(true)
                    .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                    .logoutSuccessUrl("/?logout")
                    .permitAll()
                    
                .and()
                .exceptionHandling()
                    .accessDeniedHandler(accessDeniedHandler);          
    }
}