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

	private static final Logger logger = LoggerFactory.getLogger(UserManagementService.class);

	// 日時フォーマット定義
	private static final DateTimeFormatter DTO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final DateTimeFormatter QR_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

	// システム設定定数
	private static final Integer ADMIN_USER_ID = 0;      // 管理者ID
	private static final String USER_ID_PREFIX = "U";    // QRコード等のユーザーIDプレフィックス
	private static final String QR_DATA_SEPARATOR = "-"; // QRコードデータの区切り文字

	/**
	 * 凍結されていない全ユーザーのリストを取得する。
	 * @return ユーザーDTOのリスト
	 */
	public List<UserDto> findAllUsers() {
		return userRepository.findByIsFrozenFalse().stream()
			.map(this::convertToDto)
			.collect(Collectors.toList());
	}

	/**
	 * ユーザー名を部分一致で検索する。凍結されたユーザーは対象外。
	 * @param userName 検索するユーザー名
	 * @return 検索結果のユーザーDTOリスト
	 */
	public List<UserDto> findUsersByUserName(String userName) {
		return userRepository.findByUserNameContainingAndIsFrozenFalse(userName).stream()
			.map(this::convertToDto)
			.collect(Collectors.toList());
	}

	/**
	 * ユーザーIDでユーザーエンティティを検索する。
	 * @param userId 検索するユーザーID
	 * @return ユーザーエンティティを含むOptional
	 */
	public Optional<User> findUserEntityById(Integer userId) {
		return userRepository.findById(userId);
	}

	/**
	 * 新規ユーザーを登録する。
	 * @param newUserDto 登録するユーザー情報のDTO
	 * @return 登録されたユーザー情報のDTO
	 */
	public UserDto addUser(UserDto newUserDto) {
		User userToSave = convertToEntity(newUserDto);
		userToSave.setUserId(null); 

		LocalDateTime uDatetime = LocalDateTime.now();
		userToSave.setUserPrintTime(uDatetime);

		User savedUser = userRepository.save(userToSave);
		
		return convertToDto(savedUser); 
	}

	/**
	 * ユーザーを物理削除する
	 * @param userId 削除するユーザーID
	 */
	public void deleteUser(Integer userId) {
		// データベースから完全に削除（物理削除）
		userRepository.deleteById(userId);
	}
	
	/**
	 * 指定されたユーザーIDのユーザーを論理削除する。
	 * Administratorアカウントは削除できない。
	 * @param userId 削除するユーザーID
	 */
	public void disableUser(Integer userId) {

		if (ADMIN_USER_ID.equals(userId)) {
			logger.warn("Administratorアカウント(userId: {})の削除が試みられましたが、処理は拒否されました。", ADMIN_USER_ID);
			return;
		}
		
		Optional<User> userOptional = userRepository.findById(userId);
		if (userOptional.isPresent()) {
			User user = userOptional.get();
			user.setIsFrozen(true); 
			userRepository.save(user);
		} else {
			logger.warn("削除対象のユーザーが見つかりません。 UserID: {}", userId);
		}
	}
	
	/**
	 * 既存のユーザー情報を更新する。
	 * Administratorアカウントは編集できない。
	 * @param editedUserDto 編集されたユーザー情報のDTO
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
			// [修正] JpaRepository の規約に従い、save() を使用する
			userRepository.save(userToUpdate);
			
			logger.info("ユーザー情報を更新しました。 UserID: {}", editedUserDto.getUserId());
			
		} else {
			// 更新対象のユーザーが見つかったなかった場合の処理
			logger.warn("更新対象のユーザーが見つかりません。 UserID: {}", editedUserDto.getUserId()); // 修正: 見つかったなかった -> 見つかりません
		}
	}

	/**
	 * UserエンティティをUserDtoに変換する。
	 * @param user 変換元のUserエンティティ
	 * @return 変換後のUserDto
	 */
	public UserDto convertToDto(User user) {
		String formattedTimestamp = (user.getUserPrintTime() != null) ? user.getUserPrintTime().format(DTO_FORMATTER) : null;
		return new UserDto(
			user.getUserId(),
			user.getUserName(),
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
	 * UserDtoをUserエンティティに変換する。
	 * @param userDto 変換元のUserDto
	 * @return 変換後のUserエンティティ
	 */
	private User convertToEntity(UserDto userDto) {
		return new User(
			userDto.getUserId(),
			userDto.getUserName(),
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
			false 
		);
	}
	
	/**
	 * ユーザーのQRコード情報（タイムスタンプ）を更新する。
	 * @param userId 更新するユーザーID
	 * @return 更新後のユーザー情報のDTO、見つからない場合はnull
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
			// [修正] JpaRepository の規約に従い、save() を使用する
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
	 * タイムスタンプを指定された値（文字列）に戻す
	 * @param userId ユーザーID
	 * @param timestampStr ロールバックする日時文字列(yyyy-MM-dd HH:mm:ss)
	 */
	public void restoreUserTimestamp(Integer userId, String timestampStr) {
		Optional<User> currentUserOptional = userRepository.findById(userId);
		if (currentUserOptional.isPresent()) {
			User user = currentUserOptional.get();
			try {
				if (timestampStr != null && !timestampStr.isEmpty()) {
					LocalDateTime dt = LocalDateTime.parse(timestampStr, DTO_FORMATTER);
					user.setUserPrintTime(dt);
				} else {
					user.setUserPrintTime(null);
				}
				userRepository.save(user);
				logger.info("ユーザーのタイムスタンプをロールバックしました。 UserID: {}", userId);
			} catch (Exception e) {
				logger.error("タイムスタンプの復元に失敗しました。 UserID: {}, Error: {}", userId, e.getMessage());
			}
		}
	}
	
	/**
	 * QRコード印刷用のデータを取得する。
	 * @param userId データを取得するユーザーID
	 * @return 表示テキストとQRコードデータを含むマップ、ユーザーが見つからない場合はnull
	 */
	public Map<String, String> getQrDataForUser(Integer userId) {
		Optional<User> userOptional = userRepository.findById(userId);
		if (userOptional.isEmpty()) {
			return null;
		}
		User user = userOptional.get();
		
		String displayText = user.getUserName();
		String qrCodeData = generateQrCodeData(USER_ID_PREFIX, user.getUserId(), user.getUserPrintTime());

		Map<String, String> qrData = new HashMap<>();
		qrData.put("displayText", displayText);
		qrData.put("qrCodeData", qrCodeData);
		
		return qrData;
	}

	/**
	 * QRコード用のデータ文字列を生成する。
	 * @param prefix プレフィックス
	 * @param id ID
	 * @param timestamp タイムスタンプ
	 * @return フォーマットされたQRコードデータ文字列
	 */
	private String generateQrCodeData(String prefix, Integer id, LocalDateTime timestamp) {
		String formattedId = String.format("%s%04d", prefix, id);
		String formattedTimestamp = (timestamp != null) ? timestamp.format(QR_TIMESTAMP_FORMATTER) : "";
		return formattedId + QR_DATA_SEPARATOR + formattedTimestamp;
	}
	
	/**
	 * QRコード認証コードを検証する。
	 * @param submittedCredential 提出された認証コード
	 * @return 認証に成功した場合はユーザーIDを含むOptional、失敗した場合は空のOptional
	 * @throws IllegalArgumentException 認証コードの形式が不正な場合
	 */
	public Optional<Integer> verifyAuthCode(String submittedCredential) throws IllegalArgumentException {
		if (submittedCredential == null || submittedCredential.trim().isEmpty()) {
			throw new IllegalArgumentException("認証コードを入力してください。");
		}

		String usernameToProcess;
		String timestampToProcess;

		if (submittedCredential.contains(QR_DATA_SEPARATOR)) {
			String[] parts = submittedCredential.split(QR_DATA_SEPARATOR, 2);
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
		if (usernameToProcess.startsWith(USER_ID_PREFIX)) {
			try {
				userIdToFind = Integer.parseInt(usernameToProcess.substring(USER_ID_PREFIX.length()));
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("ユーザーIDの形式が正しくありません。");
			}
		} else {
			 throw new IllegalArgumentException("ユーザーIDの形式が正しくありません。");
		}

		Optional<User> userOptional = userRepository.findById(userIdToFind);
		if (userOptional.isEmpty()) {
			return Optional.empty();
		}

		User user = userOptional.get();
		String storedTimestamp = user.getUserPrintTime().format(QR_TIMESTAMP_FORMATTER);

		if (!timestampToProcess.equals(storedTimestamp)) {
			return Optional.empty();
		}

		return Optional.of(user.getUserId());
	}
}