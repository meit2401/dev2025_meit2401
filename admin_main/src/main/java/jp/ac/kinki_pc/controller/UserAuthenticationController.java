package jp.ac.kinki_pc.controller;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jp.ac.kinki_pc.service.UserAuthenticationService;

@RestController
public class UserAuthenticationController {

	@Autowired
	private UserAuthenticationService userAuthenticationService;

	// application.propertiesから値を注入 (デフォルトはfalse)
	@Value("${app.auth.test-mode-qr-auth:false}")
	private boolean testModeQrAuth;

	/**
	 * フロントエンドに認証設定（テストモードの有無など）を提供するAPI
	 */
	@GetMapping("/api/auth-config")
	public ResponseEntity<Map<String, Boolean>> getAuthConfig() {
		return ResponseEntity.ok(Map.of("testModeQrAuth", testModeQrAuth));
	}

	@PostMapping("/api/verify-auth-code")
	public ResponseEntity<?> verifyAuthCode(@RequestBody Map<String, String> payload) {
		String submittedCredential = payload.get("authCode");

		try {
			// UserAuthenticationServiceのメソッドを呼び出す
			Optional<Integer> userIdOptional = userAuthenticationService.verifyAuthCode(submittedCredential);
			
			if (userIdOptional.isPresent()) {
				// 認証成功時、ユーザーIDを返す
				return ResponseEntity.ok(Map.of("userId", userIdOptional.get()));
			} else {
				// 認証失敗時 (ユーザーID不一致またはタイムスタンプ不一致)
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("ユーザーIDまたは認証コードが正しくありません。");
			}
			
		} catch (IllegalArgumentException e) {
			// 形式不正時
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		} catch (Exception e) {
			// 追加: 予期せぬエラー（DBエラーやNPEなど）
			e.printStackTrace(); // ログにスタックトレースを出力
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("サーバー内部エラーが発生しました。");
		}
	}
}