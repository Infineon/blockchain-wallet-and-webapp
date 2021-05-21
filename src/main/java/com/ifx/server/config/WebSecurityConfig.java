/**
 * MIT License
 *
 * Copyright (c) 2021 Infineon Technologies AG
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE
 */
package com.ifx.server.config;

import com.ifx.server.service.security.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import static com.ifx.server.EndpointConstants.*;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Handle request using cookie with JSESSIONID as authentication method
     * e.g. web page access
     */
    @Configuration
    public class SecurityConfig extends WebSecurityConfigurerAdapter {

        @Qualifier("userInformationService")
        @Autowired
        private UserDetailsService userDetailsService;

        @Override
        public void configure(AuthenticationManagerBuilder builder)
                throws Exception {
            builder.userDetailsService(userDetailsService);
        }

        @Override
        protected void configure(final HttpSecurity http) throws Exception {
            // Security configuration for all endpoints, comprises REST services and websockets
            http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .and()
                    .csrf()
                    /**
                     * Disable the requirement of X-CSRF-TOKEN in header for endpoints.
                     * This will affect POST only, csrf does not affect GET and permitAll()
                     */
                    .ignoringAntMatchers(WEBAPI_ACCOUNT_TRANSACT_SIGN_URL, WEBAPI_ACCOUNT_ADD_HW_URL)
                .and()
                    .authorizeRequests()
                    .antMatchers(WEBCONTENT_ROOT_URL, WEBCONTENT_HOME_URL, WEBCONTENT_ALL_STATIC_URLS, WEBCONTENT_ALL_WEBJAR_URLS,
                            WEBCONTENT_ENTRY_URL, WEBAPI_SIGN_UP_URL, WEBAPI_SIGN_IN_URL, WEBAPI_PING_URL,
                            WEBCONTENT_ERROR_URL, WEBAPI_FACTORY_RESET_URL, WEBAPI_ACCOUNT_TRANSACT_SIGN_URL,
                            WEBAPI_ACCOUNT_ADD_HW_URL).permitAll()
                    .antMatchers(WEBCONTENT_DASHBOARD_URL, WEBAPI_GET_USERNAME_URL, WEBAPI_SIGN_OUT_URL,
                            WEBAPI_WEBSOCKET_URL, WEBAPI_ACCOUNT_ADD_URL, WEBAPI_ACCOUNT_REMOVE_URL,
                            WEBAPI_ACCOUNT_INFO_URL, WEBAPI_ACCOUNT_TRANSACT_URL, WEBAPI_ACCOUNT_ADD_HW_REQ_URL,
                            WEBAPI_ACCOUNT_REFRESH_URL).hasRole("USER")
                    .anyRequest().authenticated()
                .and()
                    /**
                     * Logout management
                     * if CSRF is enabled POST must be performed on /logout
                     */
                    .logout()
                    //.logoutUrl("WEBAPI_LOGOUT_URL")
                    // to force GET to work
                    .logoutRequestMatcher(new AntPathRequestMatcher(WEBAPI_LOGOUT_URL, "GET"))
                    .logoutSuccessUrl(WEBCONTENT_ENTRY_URL)
                    .deleteCookies("JSESSIONID")
                    .invalidateHttpSession(true)

                .and()
                    /**
                     * Set SSL requirement for all channels
                     */
                    .requiresChannel()
                    .anyRequest().requiresSecure()
                .and()
                    .exceptionHandling().accessDeniedPage(WEBCONTENT_HOME_URL)
                .and()
                    .requestCache().disable();

            /**
             * custom filters
             */
            http.addFilter(new StatefulAuthenticationFilter(authenticationManager(), userDetailsService));

        }

        @Bean
        public AuthenticationManager customAuthenticationManager() throws Exception {
            return authenticationManager();
        }

        @Override
        public void configure(WebSecurity web) throws Exception {
            web.ignoring()
                    .antMatchers("/h2-console/**");
        }
    }
}
