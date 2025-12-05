// jp/ac/kinki_pc/repository/UniqueToolRepository.java
package jp.ac.kinki_pc.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jp.ac.kinki_pc.entity.UniqueTool;

@Repository
public interface UniqueToolRepository extends JpaRepository<UniqueTool, Long> {

	/**
	 */
	@Query("SELECT COALESCE(MAX(ut.uniqueNum), 0) FROM UniqueTool ut WHERE ut.basicToolId = ?1")
	int findMaxUniqueNumByBasicToolId(int basicToolId);

	/**
	 * basicToolId と uniqueNum で工具を検索する
	 * (タイムスタンプの更新ロジックはサービス層へ移行)
	 */
	Optional<UniqueTool> findByBasicToolIdAndUniqueNum(int basicToolId, int uniqueNum);

	/**
	 * ラベルコードにはミリ秒が含まれないため、指定した秒の範囲内(start <= t < end)で検索を行う
	 */
	List<UniqueTool> findByToolPrintTimeBetween(LocalDateTime start, LocalDateTime end);

	/**
	 * 命名規則により自動実装
	 */
	long countByBasicToolId(int basicToolId);

	/**
	 * 命名規則により自動実装
	 */
	@Transactional
	void deleteByBasicToolId(int basicToolId);

	/**
	 * 命名規則により自動実装 (SELECT句が * に変わりますが、DTO変換で調整)
	 */
	List<UniqueTool> findByBasicToolId(int basicToolId);

	/**
	 * 指定された基本工具IDと保管状況を持つ個体が存在するかチェックする
	 */
	boolean existsByBasicToolIdAndStorageCondition(Integer basicToolId, String storageCondition);
}