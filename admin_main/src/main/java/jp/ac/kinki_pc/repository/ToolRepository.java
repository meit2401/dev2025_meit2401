// jp/ac/kinki_pc/repository/ToolRepository.java
package jp.ac.kinki_pc.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // [追加]
import org.springframework.data.jpa.repository.Query; // [追加]
import org.springframework.stereotype.Repository;

import jp.ac.kinki_pc.entity.Tool;

@Repository
public interface ToolRepository extends JpaRepository<Tool, Integer>, JpaSpecificationExecutor<Tool> {

	/**
	 * 在庫が発注点を下回っている工具のリストを取得します。
	 * (ToolShortageAlertRepository から移行)
	 *
	 * @return 不足工具のリスト (Toolエンティティ。stcフィールドには計算されたcurrent_stockが格納されます)
	 */
	@Query(value = """
				SELECT basic_tool_id,
			   tool_category,
		       maker,
		       tool_name,
		       tool_material,
		       buyer,
		       rop,
		       stc,
			   is_frozen
			   FROM mst_tool
			   WHERE stc < rop AND is_frozen = 0;
		""", nativeQuery = true) // ネイティブSQLとして実行
	List<Tool> findShortageAlerts();
}