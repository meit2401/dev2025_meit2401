package jp.ac.kinki_pc.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.ac.kinki_pc.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
	
	/**
	 * ユーザー名を部分一致で検索する
	 */
	List<User> findByUserNameContaining(String userName);
	
	/**
	 * 凍結されていない(isFrozen = false)ユーザーを全件取得する
	 */
	List<User> findByIsFrozenFalse();

	/**
	 * 凍結されていないユーザーの中から、ユーザー名を部分一致で検索する
	 */
	List<User> findByUserNameContainingAndIsFrozenFalse(String userName);
}