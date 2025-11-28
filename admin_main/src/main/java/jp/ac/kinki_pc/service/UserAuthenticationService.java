// jp/ac/kinki_pc/service/UserAuthenticationService.java

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
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		Integer userId;
		String usernameToParse = username; // ★ パース用の変数に代入

		// ★ ユーザーIDのプレフィックス'U'を除去 (QRコード認証のロジックと合わせる)
		if (usernameToParse != null && usernameToParse.startsWith("U")) {
			if (usernameToParse.length() > 1) {
				usernameToParse = usernameToParse.substring(1); // "U"を除去
			} else {
				// "U"のみが入力された場合
				throw new UsernameNotFoundException("ユーザーIDの形式が正しくありません: " + username);
			}
		}
		
		try {
			// ★ プレフィックスを除去した文字列(usernameToParse)をIntegerに変換
			userId = Integer.parseInt(usernameToParse);
		} catch (NumberFormatException e) {
			// "U"以外の英字などが入力された場合もここでキャッチされる
			throw new UsernameNotFoundException("ユーザーIDの形式が正しくありません: " + username);
		}
		
		// 変換したInteger型のIDでDBを検索
		Optional<User> userOptional = userRepository.findById(userId);
		
		if (userOptional.isEmpty()) {
			throw new UsernameNotFoundException("ユーザーが見つかりません: " + username);
		}
		
		User user = userOptional.get();

		// PasswordRepositoryから共有パスワードを取得
		// (DBにはハッシュ化済みの値が保存されている想定)
		String password = passwordRepository.findPassword()
				.orElseThrow(() -> new UsernameNotFoundException("共有パスワードが設定されていません。"));
		
		List<GrantedAuthority> authorities = new ArrayList<>();
		if (user.getPerAdd() == 1) authorities.add(new SimpleGrantedAuthority("ROLE_REPLENISHMENT"));
		if (user.getPerUser() == 1) authorities.add(new SimpleGrantedAuthority("ROLE_USERS"));
		if (user.getPerTool() == 1) authorities.add(new SimpleGrantedAuthority("ROLE_TOOLS"));
		if (user.getPerLine() == 1) authorities.add(new SimpleGrantedAuthority("ROLE_LINE"));
		if (user.getPerAnalysis() == 1) authorities.add(new SimpleGrantedAuthority("ROLE_STOCK"));
		if (user.getPerHistory() == 1) authorities.add(new SimpleGrantedAuthority("ROLE_HISTORY"));
		if (user.getPerDb() == 1) authorities.add(new SimpleGrantedAuthority("ROLE_DATABASE"));
		if (user.getPerSetting() == 1) authorities.add(new SimpleGrantedAuthority("ROLE_SETTING"));
		
		// Spring SecurityのUserオブジェクトを構築
		return org.springframework.security.core.userdetails.User.builder()
			.username(String.valueOf(user.getUserId())) // usernameはString型である必要がある
			// 取得した共有パスワードを設定
			.password(password)
			.authorities(authorities)
			.build();
	}
}