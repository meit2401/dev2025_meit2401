// jp/ac/kinki_pc/repository/ToolRepository.java
package jp.ac.kinki_pc.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // [変更なし]
import org.springframework.data.jpa.repository.Query; // [変更なし]
import org.springframework.stereotype.Repository;

import jp.ac.kinki_pc.entity.Tool;

@Repository
public interface ToolRepository extends JpaRepository<Tool, Integer>, JpaSpecificationExecutor<Tool> {

	/**
	 * 在庫が発注点を下回っている工具のリストを取得します。
	 * (ToolShortageAlertRepository から移行)
	 *
	 * @return 不足工具のリスト (Toolエンティティ。stcフィールドには mst_tool.stc が格納されます)
	 */
	@Query(value = """
		SELECT
			b.basic_tool_id,
			b.tool_name,
			b.rop,
			b.stc, -- [変更] mst_tool.stc を直接参照
			b.tool_category,
			b.maker,
			b.tool_material,
			b.buyer
		FROM
			mst_tool b
		WHERE
			b.stc < b.rop -- [変更] 条件も mst_tool.stc を参照
		""", nativeQuery = true) // ネイティブSQLとして実行
	List<Tool> findShortageAlerts();
}