// src/main/java/jp/ac/kinki_pc/repository/HistoryProjection.java
package jp.ac.kinki_pc.repository;

import java.time.LocalDateTime;

/**
 * リポジトリ層がJPQLの結果をマッピングするためのProjectionインターフェース。
 * DTO (HistoryData) への直接依存を避けるために使用する。
 */
public interface HistoryProjection {
	LocalDateTime getProcTime(); 
	String getUserName();
	String getOperationClass();
	String getVideoPath();
	String getToolCategory();
	String getMaker();
	String getToolName();
	String getToolMaterial();
	Integer getToolNum();        // 操作個数
    String getDisplayAddress();  // コンテナ番号
    String getTrackResult();//警告無視の結果
}