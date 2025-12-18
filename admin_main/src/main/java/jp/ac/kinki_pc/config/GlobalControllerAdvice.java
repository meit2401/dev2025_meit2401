package jp.ac.kinki_pc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 全てのController共通でViewに渡す値を設定するクラス
 */
@ControllerAdvice
public class GlobalControllerAdvice {

	// application.propertiesから値を注入 (デフォルトはfalse)
	@Value("${app.auth.test-mode-qr-auth:false}")
	private boolean testModeQrAuth;

	/**
	 * 全てのThymeleafテンプレートで ${testModeQrAuth} として参照可能にする
	 * @param model モデル
	 */
	@ModelAttribute
	public void addCommonAttributes(Model model) {
		model.addAttribute("testModeQrAuth", testModeQrAuth);
	}
}