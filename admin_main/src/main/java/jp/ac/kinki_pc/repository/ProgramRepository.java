// jp/ac/kinki_pc/repository/ProgramRepository.java

package jp.ac.kinki_pc.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jp.ac.kinki_pc.entity.Program;

@Repository
public interface ProgramRepository extends JpaRepository<Program, Integer> {

	/**
     * 指定されたラインIDに紐づく全てのプログラムを取得する
     * (命名規則により自動実装)
     * (旧 findByLineId(Integer lineId) を代替)
     */
    List<Program> findByLineId(Integer lineId);

	/**
	 * 指定されたラインIDに紐づく全てのプログラムを削除する
	 * (命名規則により自動実装)
	 * (旧 deleteByLineId(Integer lineId) を代替)
	 */
	void deleteByLineId(Integer lineId);
	
	/**
	 * 指定されたラインIDに紐づくプログラムIDのリストを取得する
	 * (旧 findIdsByLineId(Integer lineId) を代替)
	 */
	@Query("SELECT p.programId FROM Program p WHERE p.lineId = ?1")
	List<Integer> findIdsByLineId(Integer lineId);
}