package jp.ac.kinki_pc.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.ac.kinki_pc.entity.Line;

/**
 * ライン情報 (mst_line) のリポジトリインターフェース
 * Spring Data JPA を使用
 */
@Repository
public interface LineRepository extends JpaRepository<Line, Integer> {
	
	/**
	 * ライン名でライン情報を検索します。
	 * (Spring Data JPA の命名規則に基づき、メソッドシグネチャだけで自動実装されます)
	 * * @param lineName 検索するライン名
	 * @return 見つかったライン情報 (Optional)
	 */
	Optional<Line> findByLineName(String lineName);

	/**
	 * 凍結されていない（有効な）ラインのリストを検索します。
	 * @return isFrozenがfalseのラインリスト
	 */
	List<Line> findByIsFrozenFalse();
}