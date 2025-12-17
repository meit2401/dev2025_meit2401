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
        復旧後、現在時刻を用いて管理者用QRコードの印刷を試みます。
    #>
    
    # 接続設定 (環境に合わせて変更してください)
    $dbUser = "root"
    $dbPass = ""
    $dbHost = "localhost"
    $dbPort = "3306"
    $dbName = "system"

    # 現在時刻の取得 (認証の一貫性を保つため、DB登録用とQRコード用で同一の時刻オブジェクトを使用)
    $now = Get-Date
    $sqlDate = $now.ToString("yyyy-MM-dd HH:mm:ss") # DB保存用 (例: 2025-12-04 01:21:47)
    $qrDate  = $now.ToString("yyyyMMddHHmmss")      # QRコード用 (例: 20251204012147)

    # ■■■ SQLクエリの構築 ■■■
    # 1. SET SESSION SQL_MODE: user_id=0 を正しく扱うために必須
    # 2. ON DUPLICATE KEY UPDATE: 既にID=0がいてもエラーにせず上書きする
    # 3. user_print_time に $sqlDate を使用
    $sql = @"
SET SESSION SQL_MODE='NO_AUTO_VALUE_ON_ZERO';
INSERT INTO mst_user (
    user_id, user_name, user_print_time, 
    per_add, per_inventory, per_user, per_tool, per_line, 
    per_analysis, per_history, per_db, per_setting, is_frozen
) VALUES (
    0, 'Administrator', '$sqlDate', 
    1, 1, 1, 1, 1, 
    1, 1, 1, 1, 0
) 
ON DUPLICATE KEY UPDATE
    user_name = 'Administrator',
    user_print_time = '$sqlDate',
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

            # ■■■ QRコード印刷機能の呼び出し ■■■
            # システム(Webアプリ)で使用しているテプラ印刷API(localhost:29108)を直接呼び出します
            try {
                Write-Host "管理者QRコードの印刷を試みています..." -ForegroundColor Cyan
                
                $tepraBaseUrl = "http://localhost:29108/api/printer"
                
                # 1. プリンタの自動選択
                $autoSelectUrl = "$tepraBaseUrl/autoselect"
                $printerInfo = Invoke-RestMethod -Uri $autoSelectUrl -Method Get -ErrorAction Stop

                if ($printerInfo.errorCode -eq 0 -and $printerInfo.printerName) {
                    $printerName = $printerInfo.printerName
                    Write-Host "使用するプリンタ: $printerName" -ForegroundColor DarkGray

                    # 2. QRコードデータの生成
                    # UserManagementService.java のロジックに合わせ、ID(0)とDB登録時刻($qrDate)を結合
                    $qrCodeData = "U0000-$qrDate"

                    # 3. 印刷パラメータの構築 (TepraPrint.jsのデフォルト値と同等)
                    # API仕様に合わせて数値を指定 (例: tapeCut=2はEACH_LABEL)
                    $printParams = @{
                        copies = 1
                        tapeCut = 2
                        halfCut = 2
                        printSpeed = 1
                        density = @{ mode = 1; value = 0 }
                        tapeID = 262  # 18mm Tape
                        priorityCutSetting = 1
                        halfCutSeparate = 1
                        marginLeftRight = 0
                        displayTapeWidth = 2
                        errorMessage = @{ mode = 2; fileOutput = 0; filePath = "" }
                        displayTransferTape = 2
                        displayPrintSetting = 2
                        cutTitle = 0
                        kanaZen = 0
                        displayPrintPreview = 1
                        stretchImage = 0
                    }

                    $body = @{
                        printParameter = $printParams
                        qrCodeData = $qrCodeData
                    } | ConvertTo-Json -Depth 10

                    # 4. 印刷実行APIのコール
                    $encodedPrinterName = [Uri]::EscapeDataString($printerName)
                    $printUrl = "$tepraBaseUrl/print/qrcode/$encodedPrinterName"
                    
                    $response = Invoke-RestMethod -Uri $printUrl -Method Post -Body $body -ContentType "application/json" -ErrorAction Stop

                    if ($response.result -eq 1) {
                        Write-Host "成功: QRコードが印刷されました (JobID: $($response.jobid))" -ForegroundColor Green
                    } else {
                        Write-Host "エラー: 印刷APIがエラーを返しました (ErrorCode: $($response.errcode))" -ForegroundColor Red
                    }

                } else {
                     Write-Host "警告: 有効なテプラプリンタが見つかりませんでした (ErrorCode: $($printerInfo.errorCode))" -ForegroundColor Yellow
                }
            }
            catch {
                Write-Host "警告: QRコード印刷システムに接続できませんでした。サービス(TEPRA Link 2等)が起動しているか確認してください。" -ForegroundColor Yellow
                Write-Host "詳細: $_" -ForegroundColor DarkGray
            }

        } else {
            Write-Host "エラー: コマンドが終了コード $LASTEXITCODE で終了しました。" -ForegroundColor Red
        }
    }
    catch {
        Write-Host "例外が発生しました: $_" -ForegroundColor Red
    }
}