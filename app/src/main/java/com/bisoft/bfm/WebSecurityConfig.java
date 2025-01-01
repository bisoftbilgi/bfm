package com.bisoft.bfm;
import org.springframework.beans.factory.annotation.Autowired;
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

import com.bisoft.bfm.helper.SymmetricEncryptionUtil;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final SymmetricEncryptionUtil symmetricEncryptionUtil;

	@Autowired
	private ConfigurationManager configurationManager;

    @Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http
		.authorizeHttpRequests(authorize -> authorize
			.anyRequest().authenticated()
		)
		.csrf(csrf -> csrf.disable())
		.formLogin(Customizer.withDefaults())
		.httpBasic(Customizer.withDefaults());
		
	return http.build();
	}

	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
	 return new BCryptPasswordEncoder();
	}

	@Bean
	public UserDetailsService userDetailsService() {
		if(this.configurationManager.getConfiguration().getIsEncrypted()){
            // password = symmetricEncryptionUtil.decrypt(password).replace("=","");
            this.configurationManager.getConfiguration().setPgPassword(symmetricEncryptionUtil.decrypt(this.configurationManager.getConfiguration().getPgPassword()));
        }
		UserDetails user =
			 User.withUsername(this.configurationManager.getConfiguration().getPgUsername())
				.password(passwordEncoder().encode(this.configurationManager.getConfiguration().getPgPassword()))
				.roles("USER")
				.build();

		return new InMemoryUserDetailsManager(user);
	}

}