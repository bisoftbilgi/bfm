package com.bisoft.bfm;
import com.bisoft.bfm.helper.SymmetricEncryptionUtil;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final SymmetricEncryptionUtil symmetricEncryptionUtil;

    @Value("${server.pguser:postgres}")
    private String username;

    @Value("${server.pgpassword:postgres}")
    private String password;

    @Value("${bfm.user-crypted:false}")
    public boolean isEncrypted;

    @Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		// http
		// 	.authorizeHttpRequests((requests) -> requests
		// 		.requestMatchers("/", "/bfm").permitAll()
		// 		.anyRequest().authenticated()
		// 	)
		// 	.formLogin((form) -> form
		// 		.loginPage("/bfm/login")
		// 		.permitAll()
		// 	)
		// 	.logout((logout) -> logout.permitAll());

		// return http.build();

		http
			.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
			.csrf(csrf -> csrf.disable())
			.formLogin(form -> form
				.successHandler(authenticationSuccessHandler()))
			.httpBasic(Customizer.withDefaults());
		
		return http.build();
	}

	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
	 return new BCryptPasswordEncoder();
	}

	@Bean
	public UserDetailsService userDetailsService() {
		if(isEncrypted){
            // password = symmetricEncryptionUtil.decrypt(password).replace("=","");
            password = symmetricEncryptionUtil.decrypt(password);
        }
		UserDetails user =
			 User.withUsername(username)
				.password(passwordEncoder().encode(password))
				.roles("USER")
				.build();

		return new InMemoryUserDetailsManager(user);
	}

	@Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        SimpleUrlAuthenticationSuccessHandler successHandler = new SimpleUrlAuthenticationSuccessHandler();
        successHandler.setDefaultTargetUrl("/bfm/index.html"); // Redirect to /home after login
        successHandler.setAlwaysUseDefaultTargetUrl(true); // Always use the default target URL
        return successHandler;
    }

}