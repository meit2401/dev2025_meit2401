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

	// QRコード認証無効化の設定値
	@Value("${app.test-mode.disable-qr-auth:false}")
	private boolean testModeQrAuth;

	// 印刷機能無効化の設定値
	@Value("${app.test-mode.disable-qr-printing:false}")
	private boolean testModeDisableQrPrinting;

	/**
	 * 全てのThymeleafテンプレートで値として参照可能にする
	 * @param model モデル
	 */
	@ModelAttribute
	public void addCommonAttributes(Model model) {
		model.addAttribute("testModeQrAuth", testModeQrAuth);
		model.addAttribute("testModeDisableQrPrinting", testModeDisableQrPrinting);
	}
}