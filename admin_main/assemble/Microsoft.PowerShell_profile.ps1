function Reset-AdminPassword {
    <#
    .SYNOPSIS
        mst_passwordテーブルのハッシュ値を強制的に初期値へリセットします。
    .DESCRIPTION
        ターゲットDB: system
        設定ハッシュ: $2a$08$FaueCujPv8IcZE7r9Ce4IuDMJffh9ViVvLevA52tUM4G6RAuaAAZi
    #>
    
    # 接続設定
    $dbUser = "root"
    $dbPass = ""      # パスワードなし
    $dbHost = "localhost"
    $dbPort = "3306"
    $dbName = "system"

    # 設定するハッシュ値
    $targetHash = '$2a$08$FaueCujPv8IcZE7r9Ce4IuDMJffh9ViVvLevA52tUM4G6RAuaAAZi'

    # SQLクエリ
    $query = "TRUNCATE TABLE mst_password; INSERT INTO mst_password (password) VALUES ('$targetHash');"

    try {
        # 基本の引数リストを作成
        $argsList = @("-u$dbUser", "-h", $dbHost, "-P", $dbPort, $dbName, "-e", $query)

        # パスワードがある場合のみ -p オプションを追加する
        if (![string]::IsNullOrEmpty($dbPass)) {
            # 配列の先頭に追加等ではなく、結合する
            $argsList = @("-u$dbUser", "-p$dbPass", "-h", $dbHost, "-P", $dbPort, $dbName, "-e", $query)
        }

        Write-Host "データベース($dbName)のパスワードハッシュを初期化しています..." -ForegroundColor Cyan
        
        # mysqlコマンドの実行
        & mysql $argsList

        if ($LASTEXITCODE -eq 0) {
            Write-Host "成功: パスワードがリセットされました。" -ForegroundColor Green
        } else {
            Write-Host "エラー: mysqlコマンドが終了コード $LASTEXITCODE で終了しました。" -ForegroundColor Red
        }
    }
    catch {
        Write-Host "例外が発生しました: $_" -ForegroundColor Red
    }
}

function Reset-AdminUser {
    <#
    .SYNOPSIS
        管理者ユーザー(Administrator)の情報を強制的に初期状態へリセットします。
        既存の操作履歴などを破壊することなく、ユーザー情報だけを修復します。
    #>
    
    # 接続設定 (環境に合わせて変更してください)
    $dbUser = "root"
    $dbPass = ""
    $dbHost = "localhost"
    $dbPort = "3306"
    $dbName = "system"

    # ■■■ SQLクエリの構築 ■■■
    # 1. SET SESSION SQL_MODE: user_id=0 を正しく扱うために必須
    # 2. ON DUPLICATE KEY UPDATE: 既にID=0がいてもエラーにせず上書きする
    $sql = @"
SET SESSION SQL_MODE='NO_AUTO_VALUE_ON_ZERO';
INSERT INTO mst_user (
    user_id, user_name, user_print_time, 
    per_add, per_inventory, per_user, per_tool, per_line, 
    per_analysis, per_history, per_db, per_setting, is_frozen
) VALUES (
    0, 'Administrator', '2025-12-04 01:21:47', 
    1, 1, 1, 1, 1, 
    1, 1, 1, 1, 0
) 
ON DUPLICATE KEY UPDATE
    user_name = 'Administrator',
    user_print_time = '2025-12-04 01:21:47',
    per_add = 1,
    per_inventory = 1,
    per_user = 1,
    per_tool = 1,
    per_line = 1,
    per_analysis = 1,
    per_history = 1,
    per_db = 1,
    per_setting = 1,
    is_frozen = 0;
"@

    try {
        # 引数リストの作成
        $argsList = @("-u$dbUser", "-h", $dbHost, "-P", $dbPort, $dbName, "-e", $sql)

        # パスワードがある場合のみ -p を追加
        if (![string]::IsNullOrEmpty($dbPass)) {
            $argsList = @("-u$dbUser", "-p$dbPass", "-h", $dbHost, "-P", $dbPort, $dbName, "-e", $sql)
        }

        Write-Host "管理者ユーザー(Administrator: ID 0)を復旧しています..." -ForegroundColor Cyan
        
        # mysqlコマンドの実行
        & mysql $argsList

        if ($LASTEXITCODE -eq 0) {
            Write-Host "成功: Administratorユーザーは正常な状態にリセットされました。" -ForegroundColor Green
        } else {
            Write-Host "エラー: コマンドが終了コード $LASTEXITCODE で終了しました。" -ForegroundColor Red
        }
    }
    catch {
        Write-Host "例外が発生しました: $_" -ForegroundColor Red
    }
}