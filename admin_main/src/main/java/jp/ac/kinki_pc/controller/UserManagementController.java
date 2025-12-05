package jp.ac.kinki_pc.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jp.ac.kinki_pc.dto.UserDto;
import jp.ac.kinki_pc.service.UserManagementService;

@Controller
@RequestMapping("/user")
public class UserManagementController {

	@Autowired
	private UserManagementService userManagementService;
	
	/**
	 * ユーザー一覧画面を表示する
	 * @param model モデルオブジェクト
	 * @return UserManagement.html
	 */
	@GetMapping
	public String listUsers(Model model) {
		// UserManagementServiceを呼び出して全ユーザー情報を取得
		List<UserDto> users = userManagementService.findAllUsers();
		model.addAttribute("users", users);
		// 新規登録モーダルのために空のUserDtoオブジェクトを準備
		model.addAttribute("newUser", new UserDto());
		return "UserManagement";
	}
	
	/**
	 * 新規ユーザー登録処理を行い、登録されたユーザー情報をJSONで返す
	 * @param newUserDto HTMLフォームから送られてきたUserDtoオブジェクト
	 * @return 登録されたユーザー情報を含むResponseEntity
	 */
	@PostMapping("/add")
	@ResponseBody // JSONレスポンスを返すために@ResponseBodyアノテーションを追加
	public ResponseEntity<UserDto> addUser(@ModelAttribute UserDto newUserDto) {
		// UserManagementServiceを呼び出して新規ユーザーを保存し、保存されたDTOを受け取る
		UserDto savedUserDto = userManagementService.addUser(newUserDto);
		// 保存されたエンティティをDTOに変換してクライアントに返す
		// UserDto savedUserDto = userManagementService.convertToDto(savedUser); // 削除
		return ResponseEntity.ok(savedUserDto);
	}
	
	/**
	 * ユーザー削除処理を行う
	 * @param userId 削除するユーザーID
	 * @return 削除結果を返す
	 */
	@PostMapping("/delete")
	@ResponseBody
	public ResponseEntity<String> deleteUser(@RequestParam("userId") Integer userId) { // 型をIntegerに変更
		userManagementService.deleteUser(userId);
		return ResponseEntity.ok("deleted");
	}
	
	/**
	 * ユーザー編集処理を行う
	 * @param editedUserDto 編集されたユーザー情報
	 * @return 更新結果を返す
	 */
	@PostMapping("/edit")
	@ResponseBody
	public ResponseEntity<String> editUser(@ModelAttribute UserDto editedUserDto) {
		// UserManagementServiceを呼び出してユーザー情報を更新
		userManagementService.updateUser(editedUserDto);
		return ResponseEntity.ok("updated");
	}
	
	/**
	 * 氏名でユーザーを検索し、JSON形式で返す
	 * @param userName 検索する氏名
	 * @return 検索結果のユーザーリスト
	 */
	@GetMapping("/search")
	@ResponseBody
	public List<UserDto> searchUsers(@RequestParam("userName") String userName) {
		return userManagementService.findUsersByUserName(userName);
	}
	
	/**
	 * QRコード再印刷のためにタイムスタンプを更新し、更新後のユーザー情報と更新前のタイムスタンプを返す
	 * @param userId ユーザーID
	 * @return 更新後のユーザー情報と旧タイムスタンプを含むMap
	 */
	@PostMapping("/reprint-qr")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> reprintQrCode(@RequestParam("userId") Integer userId) {
		// ロールバック用に更新前の情報を取得しておく
		UserDto oldUser = userManagementService.findUserEntityById(userId)
				.map(userManagementService::convertToDto)
				.orElse(null);

		// Serviceから直接DTOを受け取る
		UserDto updatedUserDto = userManagementService.updateUserTimestamp(userId);
		
		if (updatedUserDto != null && oldUser != null) {
			Map<String, Object> response = new HashMap<>();
			response.put("user", updatedUserDto);
			response.put("oldTimestamp", oldUser.getUserPrintTime());
			return ResponseEntity.ok(response);
		} else {
			return ResponseEntity.notFound().build();
		}
	}
	
	/**
	 * 印刷失敗時にタイムスタンプを元に戻す処理
	 * @param userId ユーザーID
	 * @param oldTimestampStr 戻したいタイムスタンプ文字列
	 * @return 処理結果
	 */
	@PostMapping("/restore-timestamp")
	@ResponseBody
	public ResponseEntity<String> restoreTimestamp(@RequestParam("userId") Integer userId, 
			@RequestParam(value = "oldTimestamp", required = false) String oldTimestampStr) {
		userManagementService.restoreUserTimestamp(userId, oldTimestampStr);
		return ResponseEntity.ok("restored");
	}
}