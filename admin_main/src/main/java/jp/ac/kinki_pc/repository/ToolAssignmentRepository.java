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
	 */
	@Query("""
		SELECT ta
		FROM
			ToolAssignment ta
		WHERE
			ta.programId = ?1
		ORDER BY
			ta.toolNum
	""")
	List<ToolAssignment> findByProgramId(Integer programId);

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

	/**
	 * 基本工具IDを使用している割り当てが存在するかチェックする
	 */
	boolean existsByBasicToolId(Integer basicToolId);

	/**
	 * 基本工具IDに紐づく割り当てリストを取得する
	 */
	List<ToolAssignment> findByBasicToolId(Integer basicToolId);
}