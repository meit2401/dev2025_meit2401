package jp.ac.kinki_pc.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.ac.kinki_pc.dto.PasswordDto;
import jp.ac.kinki_pc.repository.PasswordRepository;

@Service
@Transactional
public class PasswordSettingService {

	@Autowired
	private PasswordRepository passwordRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	/**
	 * 共有パスワードをBcryptでハッシュ化して更新します。
	 * @param passwordDto 新しいパスワードを持つDTO
	 */
	public void updatePassword(PasswordDto passwordDto) {
		// パスワードをBcryptでハッシュ化する
		String hashedPassword = passwordEncoder.encode(passwordDto.getPassword());
		
		// ハッシュ化されたパスワードをリポジトリに渡す
		passwordRepository.updatePassword(hashedPassword);
	}
}