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

	/**
	 * パスワードエンコーダーのBean定義。
	 * BCryptアルゴリズムを使用してパスワードをハッシュ化する。
	 * @return PasswordEncoder パスワードエンコーダーのインスタンス
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	/**
	 * セキュリティフィルターチェーンの設定。
	 * 各エンドポイントへのアクセス制御、ログイン・ログアウトの設定を行う。
	 * @param http HttpSecurity オブジェクト
	 * @return SecurityFilterChain セキュリティフィルターチェーンのインスタンス
	 * @throws Exception 設定中に発生する可能性のある例外
	 */
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			// エンドポイントごとのアクセス制御設定
			.authorizeHttpRequests(auth -> auth
				// 認証なしでアクセス可能なエンドポイント
				.requestMatchers("/", "/alert", "/alert/**", 
								 "/api/tool-shortages", "/api/tool-shortages/count", "/api/verify-auth-code",
								 "/css/**", "/js/**", "/images/**").permitAll()
				// ロールベースのアクセス制御
				.requestMatchers("/users").hasRole("USERS")
				.requestMatchers("/tools").hasRole("TOOLS")
				.requestMatchers("/line").hasRole("LINE")
				.requestMatchers("/stock").hasRole("STOCK")
				.requestMatchers("/history").hasRole("HISTORY")
				.requestMatchers("/database").hasRole("DATABASE")
				.anyRequest().authenticated()
			)
			// CSRF保護を無効化
			.csrf(AbstractHttpConfigurer::disable)
			// 認証エントリポイント設定
			.exceptionHandling(exceptions -> exceptions
				.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/alert?loginRequired=true"))
			)
			// ログイン設定
			.formLogin(login -> login
				.loginProcessingUrl("/login")
				.defaultSuccessUrl("/alert", true)
				.failureUrl("/alert?error")
				.permitAll()
			)
			// ログアウト設定
			.logout(logout -> logout
				.logoutUrl("/logout")
				.logoutSuccessUrl("/alert")
				.permitAll()
			);
		
		return http.build();
	}
}