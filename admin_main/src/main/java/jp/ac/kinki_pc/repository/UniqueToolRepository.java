// jp/ac/kinki_pc/repository/UniqueToolRepository.java
package jp.ac.kinki_pc.repository;

import java.time.LocalDateTime; // 追加
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
	 * (ToolRepository から移行)
	 */
	@Query("SELECT COALESCE(MAX(ut.uniqueNum), 0) FROM UniqueTool ut WHERE ut.basicToolId = ?1")
	int findMaxUniqueNumByBasicToolId(int basicToolId);

	/**
	 * (ToolRepository から移行)
	 * basicToolId と uniqueNum で工具を検索する
	 * (タイムスタンプの更新ロジックはサービス層へ移行)
	 */
	Optional<UniqueTool> findByBasicToolIdAndUniqueNum(int basicToolId, int uniqueNum);

	/**
	 * (ToolRepository から移行)
	 * 命名規則により自動実装
	 */
	long countByBasicToolId(int basicToolId);

	/**
	 * (ToolRepository から移行)
	 * 命名規則により自動実装
	 */
	@Transactional
	void deleteByBasicToolId(int basicToolId);

	/**
	 * (ToolRepository から移行)
	 * 命名規則により自動実装 (SELECT句が * に変わりますが、DTO変換で調整)
	 */
	List<UniqueTool> findByBasicToolId(int basicToolId);

	/**
	 * [追加] 印刷日時(範囲)で検索する
	 * ビット演算による日時は秒精度のため、ミリ秒の誤差を許容するために範囲検索を行う
	 */
	List<UniqueTool> findByToolPrintTimeBetween(LocalDateTime start, LocalDateTime end);

	/**
	 * 指定された基本工具IDと保管状況を持つ個体が存在するかチェックする
	 */
	boolean existsByBasicToolIdAndStorageCondition(Integer basicToolId, String storageCondition);
}