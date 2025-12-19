function Reset-AdminPassword {
    <#
    .SYNOPSIS
        Web API経由でmst_passwordテーブル（パスワード）のみを強制的に初期値へリセットします。
    .DESCRIPTION
        ターゲット: http://localhost:8080/api/initialization/reset-admin-password
    #>
    
    $apiUrl = "http://localhost:8080/api/initialization/reset-admin-password"

    try {
        Write-Host "Web API経由で管理者パスワードを初期化しています..." -ForegroundColor Cyan
        
        $response = Invoke-RestMethod -Uri $apiUrl -Method Post -ErrorAction Stop

        if ($response.status -eq "success") {
            Write-Host "成功: $($response.message)" -ForegroundColor Green
        } else {
            Write-Host "エラー: $($response.message)" -ForegroundColor Red
        }
    }
    catch {
        Write-Host "例外が発生しました: $_" -ForegroundColor Red
        Write-Host "ヒント: Webアプリケーション(localhost:8080)が起動していることを確認してください。" -ForegroundColor Yellow
    }
}

function Reset-AdminUser {
    <#
    .SYNOPSIS
        管理者ユーザー(Administrator)の情報を修復し、QRコードを印刷します。
        処理は全てサーバー側(Web API)で行われます。
    .DESCRIPTION
        ターゲット: http://localhost:8080 (または8080)
    #>
    
    # 環境に合わせてポート番号を変更してください
    $webAppUrl = "http://localhost:8080" 
    
    $apiUrl = "$webAppUrl/api/initialization/reset-admin-user"
    
    try {
        Write-Host "管理者ユーザー情報の修復と印刷をリクエストしています..." -ForegroundColor Cyan
        Write-Host "ターゲット: $apiUrl" -ForegroundColor DarkGray
        
        # タイムアウトを少し長めに設定 (印刷処理待ちのため)
        $response = Invoke-RestMethod -Uri $apiUrl -Method Post -TimeoutSec 30 -ErrorAction Stop

        if ($response.status -eq "success") {
            Write-Host "成功: $($response.message)" -ForegroundColor Green
        } else {
            Write-Host "エラー: サーバー側でエラーが発生しました。" -ForegroundColor Red
            Write-Host "詳細: $($response.message)" -ForegroundColor Red
        }
    }
    catch {
        Write-Host "通信エラーが発生しました: $_" -ForegroundColor Red
        if ($_.Exception.Response) {
             # HTTPエラーレスポンスの中身を表示
             $reader = New-Object System.IO.StreamReader $_.Exception.Response.GetResponseStream()
             $errBody = $reader.ReadToEnd()
             Write-Host "サーバー応答: $errBody" -ForegroundColor Yellow
        }
    }
}