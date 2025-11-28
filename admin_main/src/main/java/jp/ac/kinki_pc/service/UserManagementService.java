// src/main/java/jp/ac/kinki_pc/service/UserManagementService.java

package jp.ac.kinki_pc.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jp.ac.kinki_pc.dto.UserDto;
import jp.ac.kinki_pc.entity.User;
import jp.ac.kinki_pc.repository.UserRepository;

@Service
public class UserManagementService {

	@Autowired
	private UserRepository userRepository;

	// ロガーを取得して、コンソールに出力できるようにする
	private static final Logger logger = LoggerFactory.getLogger(UserManagementService.class);

	// 画面表示用のフォーマッタ(可読性を重視)
	private static final DateTimeFormatter DTO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	
	// QRコードやログ出力用のフォーマッタ
	private static final DateTimeFormatter QR_LOG_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
	
	// 認証コード検証用のフォーマッタ
	private static final DateTimeFormatter AUTH_CODE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");


	/**
	 * 全ユーザーのリストを取得し、DTOに変換して返す
	 * [修正] 凍結されていないユーザーのみを取得するように変更
	 */
	public List<UserDto> findAllUsers() {
		return userRepository.findByIsFrozenFalse().stream()
			.map(this::convertToDto)
			.collect(Collectors.toList());
	}

	/**
	 * 新規ユーザーを登録する
	 * (変更不要)
	 */
	public UserDto addUser(UserDto newUserDto) {
		User userToSave = convertToEntity(newUserDto);
		userToSave.setUserId(null); // IDはDBで自動採番するためnullをセット

		// user_updatedとしてタイムスタンプ(LocalDateTime)を生成
		LocalDateTime uDatetime = LocalDateTime.now();
		userToSave.setUserPrintTime(uDatetime);

		// DBに保存し、自動採番されたIDを含むUserオブジェクトを受け取る
		User savedUser = userRepository.save(userToSave);
		
		// ログ出力のフォーマットを変更します
		logger.info("【新規ユーザー登録】ユーザーが登録されました。 UserID: {}, UserUpdated: {}", savedUser.getUserId(), savedUser.getUserPrintTime().format(QR_LOG_FORMATTER));
		
		return convertToDto(savedUser); // DTOに変換して返す
	}
	
	/**
	 * 指定されたユーザーIDのユーザーを削除(論理削除/凍結)する
	 * [修正] 物理削除から論理削除(isFrozen=true)に変更
	 */
	public void deleteUser(Integer userId) {
		// Administrator(userId: 0)は削除させない
		if (Integer.valueOf(0).equals(userId)) {
			logger.warn("Administratorアカウント(userId: 0)の削除が試みられましたが、処理は拒否されました。");
			return;
		}
		
		// 論理削除の実装: isFrozenフラグを立てて更新する
		Optional<User> userOptional = userRepository.findById(userId);
		if (userOptional.isPresent()) {
			User user = userOptional.get();
			user.setIsFrozen(true); // 凍結(削除扱い)
			userRepository.save(user);
			logger.info("ユーザーを論理削除(凍結)しました。 UserID: {}", userId);
		} else {
			logger.warn("削除対象のユーザーが見つかりません。 UserID: {}", userId);
		}
	}
	
	/**
	 * 既存のユーザー情報を更新する
	 * [修正] repository.update() を repository.save() に変更
	 */
	public void updateUser(UserDto editedUserDto) {
		// Administrator(userId: 0)は編集させない
		if (Integer.valueOf(0).equals(editedUserDto.getUserId())) {
			logger.warn("Administratorアカウント(userId: 0)の編集が試みられましたが、処理は拒否されました。");
			return;
		}

		// 1. まず、データベースから現在のユーザー情報を取得します
		Optional<User> currentUserOptional = userRepository.findById(editedUserDto.getUserId());
		
		if (currentUserOptional.isPresent()) {
			User userToUpdate = currentUserOptional.get();
			
			// 2. DTOから受け取った情報で、変更するフィールドのみを更新します
			//	u_datetime は元の値を維持します
			// isFrozen はここでは変更しないため、元の値(論理削除されていない状態)が維持されます
			userToUpdate.setUserName(editedUserDto.getUserName());
			// setPerOutを削除
			userToUpdate.setPerAdd(editedUserDto.getPerAdd() != null ? editedUserDto.getPerAdd() : 0);
			userToUpdate.setPerInventory(editedUserDto.getPerInventory() != null ? editedUserDto.getPerInventory() : 0);
			userToUpdate.setPerUser(editedUserDto.getPerUser() != null ? editedUserDto.getPerUser() : 0);
			userToUpdate.setPerTool(editedUserDto.getPerTool() != null ? editedUserDto.getPerTool() : 0);
			userToUpdate.setPerLine(editedUserDto.getPerLine() != null ? editedUserDto.getPerLine() : 0);
			userToUpdate.setPerAnalysis(editedUserDto.getPerAnalysis() != null ? editedUserDto.getPerAnalysis() : 0);
			userToUpdate.setPerHistory(editedUserDto.getPerHistory() != null ? editedUserDto.getPerHistory() : 0);
			userToUpdate.setPerDb(editedUserDto.getPerDb() != null ? editedUserDto.getPerDb() : 0);
			userToUpdate.setPerSetting(editedUserDto.getPerSetting() != null ? editedUserDto.getPerSetting() : 0);

			// 3. 更新した情報でデータベースを更新します
			userRepository.save(userToUpdate);
			
			logger.info("ユーザー情報を更新しました。 UserID: {}", editedUserDto.getUserId());
			
		} else {
			// 更新対象のユーザーが見つかったなかった場合の処理
			logger.warn("更新対象のユーザーが見つかりません。 UserID: {}", editedUserDto.getUserId());
		}
	}
	
	/**
	 * ユーザー名を部分一致で検索する
	 * [修正] 凍結されていないユーザーのみを検索対象とする
	 */
	public List<UserDto> findUsersByUserName(String userName) {
		return userRepository.findByUserNameContainingAndIsFrozenFalse(userName).stream()
			.map(this::convertToDto)
			.collect(Collectors.toList());
	}

	/**
	 * ユーザーIDでユーザーエンティティを検索する
	 * (変更不要)
	 */
	public Optional<User> findUserEntityById(Integer userId) {
		return userRepository.findById(userId);
	}

	/**
	 * User (Entity) を UserDto に変換する
	 * (変更不要)
	 */
	public UserDto convertToDto(User user) {
		// LocalDateTimeを指定のフォーマット(yyyy-MM-dd HH:mm:ss)の文字列に変換します
		String formattedTimestamp = (user.getUserPrintTime() != null) ? user.getUserPrintTime().format(DTO_FORMATTER) : null;
		return new UserDto(
			user.getUserId(),
			user.getUserName(),
			// フォーマットした文字列をDTOにセットします
			formattedTimestamp,
			user.getPerAdd(),
			user.getPerInventory(),
			user.getPerUser(),
			user.getPerTool(),
			user.getPerLine(),
			user.getPerAnalysis(),
			user.getPerHistory(),
			user.getPerDb(),
			user.getPerSetting()
		);
	}

	/**
	 * UserDto を User (Entity) に変換する
	 * [修正] Userエンティティのコンストラクタ変更に対応し、isFrozenにfalse(0)を設定
	 */
	private User convertToEntity(UserDto userDto) {
		return new User(
			userDto.getUserId(),
			userDto.getUserName(),
			// userUpdatedはサーバー側で生成するため、DTOからは設定しません (nullを渡します)
			null,
			userDto.getPerAdd() != null ? userDto.getPerAdd() : 0,
			userDto.getPerInventory() != null ? userDto.getPerInventory() : 0,
			userDto.getPerUser() != null ? userDto.getPerUser() : 0,
			userDto.getPerTool() != null ? userDto.getPerTool() : 0,
			userDto.getPerLine() != null ? userDto.getPerLine() : 0,
			userDto.getPerAnalysis() != null ? userDto.getPerAnalysis() : 0,
			userDto.getPerHistory() != null ? userDto.getPerHistory() : 0,
			userDto.getPerDb() != null ? userDto.getPerDb() : 0,
			userDto.getPerSetting() != null ? userDto.getPerSetting() : 0,
			false // isFrozen: 新規作成時やDTOからの変換時はデフォルトでfalse(有効)とする
		);
	}
	
	/**
	 * ユーザーのQRコード情報（タイムスタンプ）を更新する
	 * [修正] repository.update() を repository.save() に変更
	 */
	public UserDto updateUserTimestamp(Integer userId) {
		// Administrator(userId: 0)の更新を妨げていたifブロックを削除します

		Optional<User> currentUserOptional = userRepository.findById(userId);
		if (currentUserOptional.isPresent()) {
			User userToUpdate = currentUserOptional.get();
			
			// 新しいタイムスタンプ(LocalDateTime)を生成
			LocalDateTime newDatetime = LocalDateTime.now();
			userToUpdate.setUserPrintTime(newDatetime);
			
			// DBを更新
			userRepository.save(userToUpdate);
			
			// ログ出力のフォーマットを変更します
			logger.info("ユーザーのタイムスタンプを更新しました。 UserID: {}, New UserUpdated: {}", userId, newDatetime.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
			return convertToDto(userToUpdate); // DTOに変換して返す
			
		} else {
			logger.warn("タイムスタンプ更新対象のユーザーが見つかりません。 UserID: {}", userId);
			return null;
		}
	}
	
	/**
	 * QRコード印刷用のデータを取得する
	 * (変更不要)
	 */
	public Map<String, String> getQrDataForUser(Integer userId) {
		Optional<User> userOptional = userRepository.findById(userId);
		if (userOptional.isEmpty()) {
			return null;
		}
		User user = userOptional.get();
		
		String displayText = user.getUserName();
		String qrCodeData = generateQrCodeData("U", user.getUserId(), user.getUserPrintTime());

		Map<String, String> qrData = new HashMap<>();
		qrData.put("displayText", displayText);
		qrData.put("qrCodeData", qrCodeData);
		
		return qrData;
	}

	/**
	 * QRコード用のデータ文字列を生成する
	 * (変更不要)
	 */
	private String generateQrCodeData(String prefix, Integer id, LocalDateTime timestamp) {
		String formattedId = String.format("%s%04d", prefix, id);
		// QRコード用のフォーマッタ
		String formattedTimestamp = (timestamp != null) ? timestamp.format(QR_LOG_FORMATTER) : "";
		return formattedId + "-" + formattedTimestamp;
	}
	
	/**
	 * QRコード認証コードを検証する
	 * (変更不要)
	 */
	public Optional<Integer> verifyAuthCode(String submittedCredential) throws IllegalArgumentException {
		if (submittedCredential == null || submittedCredential.trim().isEmpty()) {
			throw new IllegalArgumentException("認証コードを入力してください。");
		}

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

		Optional<User> userOptional = userRepository.findById(userIdToFind);
		if (userOptional.isEmpty()) {
			// ユーザーが見つからない
			return Optional.empty();
		}

		User user = userOptional.get();
		// ユーザーの最終更新日時を"yyyyMMddHHmmss"形式にフォーマット
		String storedTimestamp = user.getUserPrintTime().format(AUTH_CODE_FORMATTER);

		// フォーマットした文字列とQRコードから読み取ったタイムスタンプ文字列を比較
		if (!timestampToProcess.equals(storedTimestamp)) {
			// タイムスタンプが一致しない
			return Optional.empty();
		}

		// 認証成功
		return Optional.of(user.getUserId());
	}
}