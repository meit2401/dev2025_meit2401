// jp/ac/kinki_pc/repository/PasswordRepository.java
// (Spring Data JPA を使わない、既存の最適な実装)

package jp.ac.kinki_pc.repository;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PasswordRepository {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/**
	 * mst_passwordテーブルからパスワードを取得します。
	 * テーブルには1件のパスワードしか存在しない想定です。
	 * @return パスワード文字列を格納したOptional
	 */
	public Optional<String> findPassword() {
		String sql = "SELECT password FROM mst_password LIMIT 1";
		try {
			String password = jdbcTemplate.queryForObject(sql, String.class);
			return Optional.ofNullable(password);
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}
	
	/**
	 * mst_passwordテーブルのパスワードを更新します。
	 * (注: WHERE句がないため、テーブルに1件のみレコードが存在する前提)
	 * @param password 新しいパスワード
	 * @return 更新された行数
	 */
	public int updatePassword(String password) {
		String sql = "UPDATE mst_password SET password = ?";
		return jdbcTemplate.update(sql, password);
	}
}