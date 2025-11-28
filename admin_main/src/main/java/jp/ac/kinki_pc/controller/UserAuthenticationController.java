// jp/ac/kinki_pc/controller/UserAuthenticationController.java

package jp.ac.kinki_pc.controller;

// import java.time.format.DateTimeFormatter; // 削除
import java.util.Map;
import java.util.Optional; // 追加

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jp.ac.kinki_pc.service.UserManagementService; // 追加

@RestController
public class UserAuthenticationController {

	// @Autowired
	// private UserRepository userRepository; // 削除

	@Autowired
	private UserManagementService userManagementService; // 追加

	@PostMapping("/api/verify-auth-code")
	public ResponseEntity<?> verifyAuthCode(@RequestBody Map<String, String> payload) {
		String submittedCredential = payload.get("authCode");

		try {
			// サービス層の認証メソッドを呼び出す
			Optional<Integer> userIdOptional = userManagementService.verifyAuthCode(submittedCredential);
			
			if (userIdOptional.isPresent()) {
				// 認証成功時、ユーザーIDを返す
				return ResponseEntity.ok(Map.of("userId", userIdOptional.get()));
			} else {
				// 認証失敗時 (ユーザーID不一致またはタイムスタンプ不一致)
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("ユーザーIDまたは認証コードが正しくありません。");
			}
			
		} catch (IllegalArgumentException e) {
			// 形式不正時 (Serviceがスローした例外をキャッチ)
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		}
	}
}