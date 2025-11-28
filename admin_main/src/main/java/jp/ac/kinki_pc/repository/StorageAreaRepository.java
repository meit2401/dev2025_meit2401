// jp/ac/kinki_pc/repository/StorageAreaRepository.java

package jp.ac.kinki_pc.repository;

import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.repository.JpaRepository; // [変更]
import org.springframework.data.jpa.repository.Query; // [追加]
import org.springframework.stereotype.Repository;

import jp.ac.kinki_pc.entity.StorageArea; // [追加]

@Repository
// [修正] JpaRepository を継承するインターフェースに変更
public interface StorageAreaRepository extends JpaRepository<StorageArea, Integer> {

	/**
	 * (ToolRepository から移行)
	 * Spring Data の命名規則により自動実装されます。
	 */
	void deleteByBasicToolId(Integer basicToolId);

	/**
	 * (ToolRepository から移行)
	 * JPQL (JPAのクエリ言語) を使用して実装します。
	 */
	@Query("SELECT s.displayAddress as display_address, COUNT(s) as address_count " +
           "FROM StorageArea s GROUP BY s.displayAddress")
	List<Map<String, Object>> getStorageAddressCounts();
}