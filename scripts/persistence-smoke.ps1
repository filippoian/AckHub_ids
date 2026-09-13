$ErrorActionPreference = "Stop"

$baseUrl = "http://localhost:8080"
$username = "persist_user"
$password = "Pwd_12345"

$registerBody = @{
    username = $username
    password = $password
} | ConvertTo-Json -Compress

try {
    $null = Invoke-RestMethod `
        -Method Post `
        -Uri "$baseUrl/api/auth/register" `
        -ContentType "application/json" `
        -Body $registerBody
    Write-Host "Register OK"
}
catch {
    $statusCode = $null
    if ($_.Exception.Response -ne $null) {
        try {
            $statusCode = [int]$_.Exception.Response.StatusCode
        }
        catch {
            try {
                $statusCode = [int]$_.Exception.Response.StatusCode.value__
            }
            catch {
                $statusCode = $null
            }
        }
    }

    if ($statusCode -eq 409) {
        Write-Host "Register 409: utente gia esistente, continuo."
    }
    else {
        throw
    }
}

$loginBody = @{
    type = "USER"
    identifier = $username
    password = $password
} | ConvertTo-Json -Compress

$login = Invoke-RestMethod `
    -Method Post `
    -Uri "$baseUrl/api/auth/login" `
    -ContentType "application/json" `
    -Body $loginBody

$token = [string]$login.token
if ([string]::IsNullOrWhiteSpace($token)) {
    throw "Login completato ma token mancante."
}

Write-Host "TOKEN=$token"
Write-Host "Ora riavvia l'app e rilancia lo script: se login funziona, la persistenza e OK"
