package jp.ac.kinki_pc.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.ac.kinki_pc.entity.Password;

/**
 * パスワード情報(mst_password)にアクセスするためのリポジトリ
 * JpaRepositoryを継承することで、findAll, save, deleteAll 等が自動的に利用可能です。
 */
@Repository
public interface PasswordRepository extends JpaRepository<Password, String> {

    /**
     * 旧互換用メソッド: テーブル内の最初のパスワードを取得します。
     * (UserAuthenticationServiceなどからの呼び出しに対応するため定義)
     */
    default Optional<String> findPassword() {
        List<Password> list = findAll();
        if (list.isEmpty()) {
            return Optional.empty();
        }
        // 1件目のレコードのパスワードを返す
        return Optional.ofNullable(list.get(0).getPassword());
    }
}