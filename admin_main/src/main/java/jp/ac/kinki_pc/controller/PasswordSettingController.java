// jp/ac/kinki_pc/controller/PasswordController.java

package jp.ac.kinki_pc.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jp.ac.kinki_pc.dto.PasswordDto;
import jp.ac.kinki_pc.service.PasswordSettingService;

@RestController
@RequestMapping("/setting/password")
public class PasswordSettingController {

	@Autowired
	private PasswordSettingService passwordSettingService;

	/**
	 * 共有パスワードを更新するAPIエンドポイント。
	 * 'SETTING'権限を持つユーザーのみアクセス可能です。
	 * @param passwordDto リクエストボディに含まれる新しいパスワード情報
	 * @return 処理結果を示すResponseEntity
	 */
	@PostMapping("/update")
	@PreAuthorize("hasRole('SETTING')")
	public ResponseEntity<Void> updatePassword(@RequestBody PasswordDto passwordDto) {
		try {
			passwordSettingService.updatePassword(passwordDto);
			return ResponseEntity.ok().build(); // 成功レスポンス (200 OK)
		} catch (Exception e) {
			// エラーロギング等をここで行う
			return ResponseEntity.internalServerError().build(); // サーバーエラーレスポンス (500)
		}
	}
}