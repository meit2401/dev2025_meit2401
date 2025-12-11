package jp.ac.kinki_pc.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import jp.ac.kinki_pc.entity.User;
import jp.ac.kinki_pc.repository.PasswordRepository;
import jp.ac.kinki_pc.repository.UserRepository;

@Service
public class UserAuthenticationService implements UserDetailsService {

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private PasswordRepository passwordRepository;
	
	/**
	 * ユーザー名（ユーザーID）に基づいてユーザー情報をロードする。
	 * ユーザーIDには、バーコードリーダーからの入力を考慮してプレフィックス"U"が含まれる場合があります。
	 * このメソッド内でプレフィックスを除去し、数値IDとして処理します。
	 * @param username ログインフォームから入力されたユーザーID（文字列）
	 * @return UserDetails Spring Securityで使用するユーザー詳細情報
	 * @throws UsernameNotFoundException ユーザーが見つからない、またはID形式が不正な場合
	 */
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		Integer userId;
		String usernameToParse = username;

		// ユーザーIDのプレフィックス'U'を除去（スキャナー入力対応）
		if (usernameToParse != null && usernameToParse.startsWith("U")) {
			if (usernameToParse.length() > 1) {
				usernameToParse = usernameToParse.substring(1); // "U"を除去
			} else {
				throw new UsernameNotFoundException("ユーザーIDの形式が正しくありません: " + username);
			}
		}
		
		try {
			// 数値型IDに変換
			userId = Integer.parseInt(usernameToParse);
		} catch (NumberFormatException e) {
			throw new UsernameNotFoundException("ユーザーIDの形式が正しくありません: " + username);
		}
		
		// DB検索
		Optional<User> userOptional = userRepository.findById(userId);
		
		if (userOptional.isEmpty()) {
			throw new UsernameNotFoundException("ユーザーが見つかりません: " + username);
		}
		
		User user = userOptional.get();

		// 共有パスワードを取得
		String password = passwordRepository.findPassword()
				.orElseThrow(() -> new UsernameNotFoundException("共有パスワードが設定されていません。"));
		
		// 権限リストの作成
		List<GrantedAuthority> authorities = new ArrayList<>();
		if (user.getPerAdd() == 1) authorities.add(new SimpleGrantedAuthority("ROLE_REPLENISHMENT"));
		if (user.getPerUser() == 1) authorities.add(new SimpleGrantedAuthority("ROLE_USERS"));
		if (user.getPerTool() == 1) authorities.add(new SimpleGrantedAuthority("ROLE_TOOLS"));
		if (user.getPerLine() == 1) authorities.add(new SimpleGrantedAuthority("ROLE_LINE"));
		if (user.getPerAnalysis() == 1) authorities.add(new SimpleGrantedAuthority("ROLE_STOCK"));
		if (user.getPerHistory() == 1) authorities.add(new SimpleGrantedAuthority("ROLE_HISTORY"));
		if (user.getPerDb() == 1) authorities.add(new SimpleGrantedAuthority("ROLE_DATABASE"));
		if (user.getPerSetting() == 1) authorities.add(new SimpleGrantedAuthority("ROLE_SETTING"));
		
		// UserDetailsオブジェクトを返却
		return org.springframework.security.core.userdetails.User.builder()
			.username(String.valueOf(user.getUserId()))
			.password(password)
			.authorities(authorities)
			.build();
	}
}