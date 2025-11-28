// src/main/java/jp/ac/kinki_pc/repository/UserRepository.java

package jp.ac.kinki_pc.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository; // [変更]
import org.springframework.stereotype.Repository;

import jp.ac.kinki_pc.entity.User;

@Repository
// [修正] JpaRepository を継承するインターフェースに変更
public interface UserRepository extends JpaRepository<User, Integer> {
	
	/**
	 * ユーザー名を部分一致で検索する
	 * (Spring Data JPA の命名規則により自動実装)
	 * (旧 findByUserNameContaining(String userName) を代替)
	 */
	List<User> findByUserNameContaining(String userName);
}