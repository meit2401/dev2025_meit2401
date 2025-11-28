// jp/ac/kinki_pc/config/SecurityConfig.java

package jp.ac.kinki_pc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(auth -> auth
				// 認証コード検証APIのエンドポイントをpermitAllに追加
				.requestMatchers("/", "/api/verify-auth-code", "/alert", "/alert/**", 
								 "/api/tool-shortages", "/api/tool-shortages/count",
								 "/css/**", "/js/**", "/images/**").permitAll()
				.requestMatchers("/users").hasRole("USERS")
				.requestMatchers("/tools").hasRole("TOOLS")
				.requestMatchers("/line").hasRole("LINE")
				.requestMatchers("/stock").hasRole("STOCK")
				.requestMatchers("/history").hasRole("HISTORY")
				.requestMatchers("/database").hasRole("DATABASE")
				.anyRequest().authenticated()
			)
			.csrf(AbstractHttpConfigurer::disable)
			.exceptionHandling(exceptions -> exceptions
				.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/alert?loginRequired=true"))
			)
			.formLogin(login -> login
				.loginProcessingUrl("/login")
				.defaultSuccessUrl("/alert", true)
				.failureUrl("/alert?error")
				.permitAll()
			)
			.logout(logout -> logout
				.logoutUrl("/logout")
				.logoutSuccessUrl("/alert")
				.permitAll()
			);
		
		return http.build();
	}
}