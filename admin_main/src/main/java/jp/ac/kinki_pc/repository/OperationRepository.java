// src/main/java/jp/ac/kinki_pc/repository/OperationRepository.java

package jp.ac.kinki_pc.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.ac.kinki_pc.entity.Operation;

@Repository
public interface OperationRepository extends JpaRepository<Operation, Integer> {
	
	/**
	 * findOperationHistory メソッドが返す JPA Projection (射影) インターフェース。
	 * Service 層で DTO にマッピングするために使用されます。
	 */
	public interface OperationHistoryProjection {
		LocalDateTime getProcTime();
		String getUserName();
		String getOperationClass();
	}

	/**
	 * 氏名、年度月、作業内容を条件に、関連する工具情報を含む操作履歴を検索します。
	 * (HistoryProjection インターフェースのリストを返します)
	 * @param year 年
	 * @param month 月
	 * @param username ユーザー名 (null許容)
	 * @param operationContent 作業内容 (null許容)
	 * @return 条件に一致する操作履歴 (HistoryProjectionのリスト)
	 */
	@Query("SELECT " +
		   "  ro.procTime AS procTime, " +
		   "  mu.userName AS userName, " +
		   "  ro.operationClass AS operationClass, " +
		   "  ro.videoPath AS videoPath, " +
		   "  mt.toolCategory AS toolCategory, " +
		   "  mt.maker AS maker, " +
		   "  mt.toolName AS toolName, " +
		   "  mt.toolMaterial AS toolMaterial " +
		   "FROM Operation ro, User mu, StorageArea sa, Tool mt " +
		   "WHERE ro.userId = mu.userId " +
		   "  AND ro.storageAreaId = sa.storageAreaId " +
		   "  AND sa.basicToolId = mt.basicToolId " +
		   "  AND YEAR(ro.procTime) = :year " +
		   "  AND MONTH(ro.procTime) = :month " +
		   "  AND (:username IS NULL OR mu.userName = :username) " +
		   "  AND (:operationContent IS NULL OR ro.operationClass = :operationContent) " +
		   "ORDER BY ro.procTime DESC")
	List<HistoryProjection> findHistoryWithToolDetails(
		@Param("year") Integer year, 
		@Param("month") Integer month, 
		@Param("username") String username, 
		@Param("operationContent") String operationContent
	);
	
	
	/**
	 * 履歴(rec_operation)テーブルに存在する操作履歴の「年月」を重複なく取得します。
	 * @return 'YYYY-MM' 形式の年月のリスト
	 */
	@Query("SELECT DISTINCT FUNCTION('DATE_FORMAT', ro.procTime, '%Y-%m') " +
		   "FROM Operation ro " +
		   "ORDER BY 1 DESC")
	List<String> findHistoryCandidate();

	// --- 旧 DatabaseRepository より統合されたメソッド ---

	/**
	 * 操作履歴(rec_operation)とユーザー名(mst_user)を結合して取得します。
	 * (DTO ではなく、OperationHistoryProjection インターフェースのリストを返します)
	 * @param end 終了日時 (この日時"以前" <=) (null許容)
	 * @param newerThan 開始日時 (この日時"より後" >) (null許容)
	 * @return OperationHistoryProjection のリスト
	 */
	@Query("SELECT " +
		   "  ro.procTime AS procTime, " +
		   "  mu.userName AS userName, " +
		   "  ro.operationClass AS operationClass " +
		   "FROM Operation ro, User mu " +
		   "WHERE ro.userId = mu.userId " +
		   "  AND (:newerThan IS NULL OR ro.procTime > :newerThan) " +
		   "  AND (:end IS NULL OR ro.procTime <= :end) " +
		   "ORDER BY ro.procTime ASC")
	List<OperationHistoryProjection> findOperationHistory(
		@Param("end") LocalDateTime end,
		@Param("newerThan") LocalDateTime newerThan
	);

	/**
	 * rec_operation テーブルから最も古い proc_time (LocalDateTime) を取得します。
	 * @return 最も古い LocalDateTime (レコードがない場合は null)
	 */
	@Query("SELECT MIN(ro.procTime) FROM Operation ro")
	LocalDateTime findOldestOperationTimestamp();

}