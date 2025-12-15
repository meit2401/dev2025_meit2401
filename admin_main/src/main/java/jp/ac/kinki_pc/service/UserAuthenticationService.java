package jp.ac.kinki_pc.service;

import java.time.format.DateTimeFormatter;
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

	// 認証コードのタイムスタンプフォーマット
	private static final DateTimeFormatter AUTH_CODE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private PasswordRepository passwordRepository;
	
	 /**
	 * ユーザーIDに基づいてユーザー情報をロードする。
	 * ユーザーIDには、バーコードリーダーからの入力を考慮してプレフィックス"U"が含まれる場合がある。
	 * このメソッド内でプレフィックスを除去し、数値IDとして処理する。
	 * @param username ログインフォームから入力されたユーザーID（文字列）
	 * @return UserDetails Spring Securityで使用するユーザー詳細情報
	 * @throws UsernameNotFoundException ユーザーが見つからない、またはID形式が不正な場合
	 */
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		Integer userId;
		String usernameToParse = username;

		 // ユーザーIDのプレフィックス'U'を除去（スキャナー入力対応）
		if (usernameToParse != null && usernameToParse.startsWith("U")) {	// "U"で始まる場合
			if (usernameToParse.length() > 1) {								// "U"の後に数字が続く場合
				usernameToParse = usernameToParse.substring(1); // "U"を除去
			} else {
				throw new UsernameNotFoundException("ユーザーIDの形式が正しくありません: " + username); // "U"のみの場合
			}
		}
		
		 // ユーザーIDを数値に変換
		try {
			userId = Integer.parseInt(usernameToParse); // 数値型IDに変換
		} catch (NumberFormatException e) {
			throw new UsernameNotFoundException("ユーザーIDの形式が正しくありません: " + username); // 変換に失敗した場合
		}
		
		 // DB検索
		Optional<User> userOptional = userRepository.findById(userId);
		
		 // ユーザー存在チェック
		if (userOptional.isEmpty()) {
			throw new UsernameNotFoundException("ユーザーが見つかりません: " + username); // ユーザーが存在しない場合
		}
		
		 // ユーザー情報取得
		User user = userOptional.get();

		 // 共有パスワードを取得
		String password = passwordRepository.findPassword()
				.orElseThrow(() -> new UsernameNotFoundException("共有パスワードが設定されていません。")); // パスワードが設定されていない場合
		
		 // 権限リストの作成
		List<GrantedAuthority> authorities = new ArrayList<>();
		if (user.getPerUser() == 1) authorities.add(new SimpleGrantedAuthority("ROLE_USERS"));      // ユーザー管理権限
		if (user.getPerTool() == 1) authorities.add(new SimpleGrantedAuthority("ROLE_TOOLS"));      // 工具管理権限
		if (user.getPerLine() == 1) authorities.add(new SimpleGrantedAuthority("ROLE_LINE"));       // ライン管理権限
		if (user.getPerAnalysis() == 1) authorities.add(new SimpleGrantedAuthority("ROLE_STOCK"));  // 工具分析権限
		if (user.getPerHistory() == 1) authorities.add(new SimpleGrantedAuthority("ROLE_HISTORY")); // 操作履歴権限
		if (user.getPerDb() == 1) authorities.add(new SimpleGrantedAuthority("ROLE_DATABASE"));     // データベース管理権限
		if (user.getPerSetting() == 1) authorities.add(new SimpleGrantedAuthority("ROLE_SETTING")); // 設定管理権限
		
		 // UserDetailsオブジェクトを返却
		return org.springframework.security.core.userdetails.User.builder()
			.username(String.valueOf(user.getUserId()))
			.password(password)
			.authorities(authorities)
			.build();
	}

	/**
	 * QRコード認証コードを検証する
	 * @param submittedCredential QRコードから読み取った文字列
	 * @return 認証成功時はユーザーID(Optional)、失敗時はOptional.empty()
	 * @throws IllegalArgumentException フォーマット不正時
	 */
	public Optional<Integer> verifyAuthCode(String submittedCredential) throws IllegalArgumentException {
		// 追加: 空白除去とNullチェック
		if (submittedCredential == null || submittedCredential.trim().isEmpty()) {
			throw new IllegalArgumentException("認証コードを入力してください。");
		}
		// 追加: 入力値の前後の空白を除去 (スキャナーによる改行コード混入対策)
		submittedCredential = submittedCredential.trim();

		String usernameToProcess;
		String timestampToProcess;

		// '-'で分割してユーザーIDとタイムスタンプを取得
		if (submittedCredential.contains("-")) {
			// splitの第二引数に2を指定することで、分割数を最大2に制限します
			String[] parts = submittedCredential.split("-", 2);
			if (parts.length == 2) {
				usernameToProcess = parts[0];
				timestampToProcess = parts[1];
			} else {
				throw new IllegalArgumentException("認証コードの形式が正しくありません。");
			}
		} else {
			throw new IllegalArgumentException("認証コードの形式が正しくありません。");
		}

		Integer userIdToFind;
		// ユーザーIDのプレフィックス'U'を除去してIntegerに変換
		if (usernameToProcess.startsWith("U")) {
			try {
				userIdToFind = Integer.parseInt(usernameToProcess.substring(1));
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("ユーザーIDの形式が正しくありません。");
			}
		} else {
			 throw new IllegalArgumentException("ユーザーIDの形式が正しくありません。");
		}

		// DB検索
		Optional<User> userOptional = userRepository.findById(userIdToFind);
		if (userOptional.isEmpty()) {
			// ユーザーが見つからない
			return Optional.empty();
		}

		User user = userOptional.get();

		// 追加: userPrintTimeがnullの場合（QRコードが未発行、またはDBに日時がない場合）は認証失敗とする
		// これにより NullPointerException を回避します
		if (user.getUserPrintTime() == null) {
			return Optional.empty();
		}

		// ユーザーの最終更新日時を"yyyyMMddHHmmss"形式にフォーマット
		String storedTimestamp = user.getUserPrintTime().format(AUTH_CODE_FORMATTER);

		// フォーマットした文字列とQRコードから読み取ったタイムスタンプ文字列を比較
		// 追加: timestampToProcessも念のためtrimする
		if (!timestampToProcess.trim().equals(storedTimestamp)) {
			// タイムスタンプが一致しない
			return Optional.empty();
		}

		// 認証成功
		return Optional.of(user.getUserId());
	}
}