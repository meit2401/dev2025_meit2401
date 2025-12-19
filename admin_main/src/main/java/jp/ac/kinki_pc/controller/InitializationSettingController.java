package jp.ac.kinki_pc.controller;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import jp.ac.kinki_pc.service.PasswordSettingService;
import jp.ac.kinki_pc.service.UserManagementService;

/**
 * システムメンテナンス用APIコントローラー
 */
@RestController
@RequestMapping("/api/initialization")
public class InitializationSettingController {

    private final UserManagementService userManagementService;
    private final PasswordSettingService passwordSettingService;

    @Autowired
    public InitializationSettingController(UserManagementService userManagementService, PasswordSettingService passwordSettingService) {
        this.userManagementService = userManagementService;
        this.passwordSettingService = passwordSettingService;
    }

    // ... resetAdminPassword メソッドは変更なし ...
    @PostMapping(value = "/reset-admin-password", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<Map<String, String>> resetAdminPassword() {
        Map<String, String> response = new HashMap<>();
        try {
            passwordSettingService.resetAdminPassword();
            response.put("status", "success");
            response.put("message", "管理者パスワードが初期化されました。");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "パスワードリセット失敗: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // ユーザーリセット & 印刷 (修正版)
    @PostMapping(value = "/reset-admin-user", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<Map<String, String>> resetAdminUser() {
        Map<String, String> response = new HashMap<>();
        try {
            // 1. DB情報の修復 (ネイティブSQL実行)
            userManagementService.resetAdminUser();

            // 2. 印刷データの準備
            Path templatePath = Paths.get("src/main/resources/static/qr_template.lw1");
            if (!Files.exists(templatePath)) {
                throw new RuntimeException("テンプレートファイルが見つかりません: " + templatePath.toAbsolutePath());
            }
            byte[] templateBytes = Files.readAllBytes(templatePath);
            String templateBase64 = Base64.getEncoder().encodeToString(templateBytes);

            // CSVデータの生成
            Map<String, String> qrData = userManagementService.getQrDataForUser(0);
            String qrCodeData = qrData.get("qrCodeData");
            String displayText = qrData.get("displayText"); 
            
            String safeDisplayText = displayText.replaceAll("[\r\n]", " ");
            String safeQrCodeData  = qrCodeData.replaceAll("[\r\n]", "");

            String csvContent = "\"" + safeDisplayText + "\",\"" + safeQrCodeData + "\"\r\n";
            
            // ★修正点: 文字コードを Windows-31J (Shift-JIS) に指定
            String csvBase64 = Base64.getEncoder().encodeToString(csvContent.getBytes(Charset.forName("Windows-31J")));

            // 3. テプラAPIへの送信
            RestTemplate restTemplate = new RestTemplate();
            String tepraBaseUrl = "http://localhost:29108/api/printer";

            // プリンタ自動選択
            Map<String, Object> printerInfo = restTemplate.getForObject(tepraBaseUrl + "/autoselect", Map.class);
            if (printerInfo == null || printerInfo.get("printerName") == null) {
                throw new RuntimeException("有効なテプラプリンタが見つかりませんでした。");
            }
            String printerName = (String) printerInfo.get("printerName");

            // リクエストボディ構築
            Map<String, Object> requestBody = new HashMap<>();
            
            Map<String, Object> printParams = new HashMap<>();
            printParams.put("copies", 1);
            printParams.put("tapeCut", 2); 
            printParams.put("halfCut", 2); 
            printParams.put("printSpeed", 1);
            printParams.put("tapeID", 262); 
            printParams.put("density", Map.of("mode", 1, "value", 0));
            
            Map<String, Object> printFile = new HashMap<>();
            printFile.put("templateFile", Map.of("fileName", "qr_template.lw1", "base64Str", templateBase64));
            printFile.put("csvFile", Map.of("fileName", "qr_data.csv", "base64Str", csvBase64));

            requestBody.put("printParameter", printParams);
            requestBody.put("printFile", printFile);

            String printUrl = tepraBaseUrl + "/print/" + printerName;
            Map<String, Object> printResult = restTemplate.postForObject(printUrl, requestBody, Map.class);

            if (printResult != null && Integer.valueOf(1).equals(printResult.get("result"))) {
                response.put("status", "success");
                response.put("message", "管理者情報の修復とQRコード印刷が完了しました。(JobID: " + printResult.get("jobid") + ")");
                return ResponseEntity.ok(response);
            } else {
                throw new RuntimeException("印刷APIエラー: " + (printResult != null ? printResult.get("errcode") : "null"));
            }

        } catch (Exception e) {
            e.printStackTrace(); 
            response.put("status", "error");
            response.put("message", "処理中にエラーが発生しました: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}