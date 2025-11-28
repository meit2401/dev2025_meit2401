// jp/ac/kinki_pc/repository/ToolAssignmentRepository.java

package jp.ac.kinki_pc.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jp.ac.kinki_pc.entity.ToolAssignment; 

@Repository
public interface ToolAssignmentRepository extends JpaRepository<ToolAssignment, Integer> {

	/**
	 * 指定されたプログラムIDに紐づく全ての工具割り当て情報（エンティティ）を取得する
	 * [修正] 戻り値を List<ToolAssignmentDto> から List<ToolAssignment> に変更。
	 * JPQLを、エンティティ(ta)をSELECTするように変更。
	 */
	@Query("""
		SELECT ta
		FROM
			ToolAssignment ta
		WHERE
			ta.programId = ?1
		ORDER BY
			ta.toolNum
	""") // [修正] DTOコンストラクタ式を削除 / [修正] ORDER BY を TNN 形式対応
	List<ToolAssignment> findByProgramId(Integer programId); // [修正] 戻り値を変更

	/**
	 * 指定されたプログラムIDに紐づく全ての工具割り当て情報を削除する
	 */
	void deleteByProgramId(Integer programId);
	
	/**
	 * プログラムIDとツール番号で既存の割り当てエンティティを検索する
	 */
	Optional<ToolAssignment> findByProgramIdAndToolNum(Integer programId, String toolNum);

	/**
	 * (新規) 指定されたプログラムIDとツール番号に紐づく工具割り当てを削除する
	 */
	void deleteByProgramIdAndToolNum(Integer programId, String toolNum);
	
	/**
	 * 指定されたプログラムIDのリストに紐づく全ての工具割り当てを削除する
	 */
	void deleteByProgramIdIn(List<Integer> programIds);
}