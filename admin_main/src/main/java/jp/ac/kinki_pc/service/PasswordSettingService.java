package jp.ac.kinki_pc.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.ac.kinki_pc.dto.PasswordDto;
import jp.ac.kinki_pc.entity.Password;
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
        
        // 既存のデータを全て削除し、新しいパスワードを保存する
        passwordRepository.deleteAll();
        
        Password newPassword = new Password();
        newPassword.setPassword(hashedPassword);
        
        passwordRepository.save(newPassword);
    }

    /**
     * パスワードテーブルを全消去し、初期ハッシュ値を設定します。
     * (メンテナンス機能用)
     */
    public void resetAdminPassword() {
        // 既存データを全て削除
        passwordRepository.deleteAll();

        // 初期パスワードレコードの作成
        Password defaultPassword = new Password();
        // $2a$08$FaueCujPv8IcZE7r9Ce4IuDMJffh9ViVvLevA52tUM4G6RAuaAAZi
        defaultPassword.setPassword("$2a$08$FaueCujPv8IcZE7r9Ce4IuDMJffh9ViVvLevA52tUM4G6RAuaAAZi");
        
        // 保存
        passwordRepository.save(defaultPassword);
    }
}