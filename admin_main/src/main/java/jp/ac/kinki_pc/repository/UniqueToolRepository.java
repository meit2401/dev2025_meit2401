// jp/ac/kinki_pc/repository/UniqueToolRepository.java
package jp.ac.kinki_pc.repository;

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
	 */
	Optional<UniqueTool> findByBasicToolIdAndUniqueNum(int basicToolId, int uniqueNum);

	/**
	 * (ToolRepository から移行)
	 */
	long countByBasicToolId(int basicToolId);

	/**
	 * (ToolRepository から移行)
	 */
	@Transactional
	void deleteByBasicToolId(int basicToolId);

	/**
	 * (ToolRepository から移行)
	 */
	List<UniqueTool> findByBasicToolId(int basicToolId);

	/**
	 * 指定された基本工具IDと保管状況を持つ個体が存在するかチェックする
	 */
	boolean existsByBasicToolIdAndStorageCondition(Integer basicToolId, String storageCondition);
}