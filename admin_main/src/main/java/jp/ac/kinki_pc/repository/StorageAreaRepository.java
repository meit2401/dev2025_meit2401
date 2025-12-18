package jp.ac.kinki_pc.repository;

import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jp.ac.kinki_pc.entity.StorageArea;

@Repository
public interface StorageAreaRepository extends JpaRepository<StorageArea, Integer> {

	/**
	 * (ToolRepository から移行)
	 * Spring Data の命名規則により自動実装されます。
	 */
	void deleteByBasicToolId(Integer basicToolId);

	/**
	 * (ToolRepository から移行)
	 * JPQL (JPAのクエリ言語) を使用して実装します。
	 * [修正] mst_toolのis_frozenが1(無効化)のものはカウントから除外する
	 */
	@Query("SELECT s.displayAddress as display_address, COUNT(s) as address_count " +
           "FROM StorageArea s, Tool t " +
           "WHERE s.basicToolId = t.basicToolId " +
           "AND t.isFrozen = false " +
           "GROUP BY s.displayAddress")
	List<Map<String, Object>> getStorageAddressCounts();

	/**
	 * 指定した数より多いケース在庫数を持つレコードが存在するかチェック
	 */
	boolean existsByBasicToolIdAndToolcaseStcNumGreaterThan(Integer basicToolId, Integer toolcaseStcNum);

	/**
	 * 指定した数より多いケース在庫数を持つレコードを検索して取得する
	 */
	List<StorageArea> findByBasicToolIdAndToolcaseStcNumGreaterThan(Integer basicToolId, Integer toolcaseStcNum);
}