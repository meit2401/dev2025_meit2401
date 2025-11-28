// jp/ac/kinki_pc/repository/StorageAreaRepository.java

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
	 */
	void deleteByBasicToolId(Integer basicToolId);

	/**
	 * (ToolRepository から移行)
	 */
	@Query("SELECT s.displayAddress as display_address, COUNT(s) as address_count " +
           "FROM StorageArea s GROUP BY s.displayAddress")
	List<Map<String, Object>> getStorageAddressCounts();

	/**
	 * 指定した数より多いケース在庫数を持つレコードが存在するかチェック
	 */
	boolean existsByBasicToolIdAndToolcaseStcNumGreaterThan(Integer basicToolId, Integer toolcaseStcNum);
}