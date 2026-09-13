param(
  [string]$BaseUrl = "http://localhost:8080",
  [string]$Username = "A",
  [string]$Password = "A"
)

# Esecuzione:
# powershell -ExecutionPolicy Bypass -File .\scripts\auth-smoke.ps1

$ErrorActionPreference = "Stop"

function Get-ErrorBody([System.Management.Automation.ErrorRecord]$Err) {
  if (-not $Err.Exception.Response) {
    return $null
  }

  try {
    $stream = $Err.Exception.Response.GetResponseStream()
    if (-not $stream) {
      return $null
    }
    $reader = New-Object System.IO.StreamReader($stream)
    return $reader.ReadToEnd()
  } catch {
    return $null
  }
}

function InvokeJson([string]$Method, [string]$Url, [hashtable]$BodyHashtable, [hashtable]$Headers = @{}) {
  $json = $null
  if ($null -ne $BodyHashtable) {
    $json = $BodyHashtable | ConvertTo-Json -Compress
  }

  try {
    return Invoke-RestMethod -Method $Method -Uri $Url -Headers $Headers -ContentType "application/json" -Body $json
  } catch {
    $statusCode = $null
    if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
      $statusCode = [int]$_.Exception.Response.StatusCode
    }

    $errorBody = Get-ErrorBody $_
    if ($errorBody) {
      Write-Host $errorBody
    }

    if ($statusCode -in @(400, 409, 404)) {
      return $null
    }

    if ($statusCode) {
      Write-Host ("HTTP {0} su {1}" -f $statusCode, $Url)
      return $null
    }

    Write-Host ("Richiesta fallita su {0}" -f $Url)
    return $null
  }
}

Write-Host "Register..."
$registerBody = @{
  username = $Username
  password = $Password
}
$registerResponse = InvokeJson -Method "POST" -Url "$BaseUrl/api/auth/register" -BodyHashtable $registerBody
if ($null -eq $registerResponse) {
  Write-Host "Utente gia presente, continuo."
} else {
  Write-Host ("Registrazione OK: userId={0}" -f $registerResponse.profile.userId)
}

Write-Host "Login..."
$loginBody = @{
  type = "USER"
  identifier = $Username
  password = $Password
}
$loginResponse = InvokeJson -Method "POST" -Url "$BaseUrl/api/auth/login" -BodyHashtable $loginBody
if ($null -eq $loginResponse) {
  Write-Host "Login fallito."
  exit 1
}

$loginJson = $loginResponse | ConvertTo-Json -Compress
Write-Host $loginJson

$token = $loginResponse.token
if (-not [string]::IsNullOrWhiteSpace($token)) {
  Write-Host ("TOKEN={0}" -f $token)

  Write-Host "Logout..."
  $logoutHeaders = @{ "X-Session-Token" = $token }
  $logoutResponse = InvokeJson -Method "POST" -Url "$BaseUrl/api/auth/logout" -BodyHashtable @{} -Headers $logoutHeaders
  if ($null -ne $logoutResponse) {
    Write-Host "Logout OK"
  } else {
    Write-Host "Logout non disponibile o fallito."
  }
}
