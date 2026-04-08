param(
    [string]$ConnectUrl = "http://localhost:8083",
    [string]$ConfigPath = "infra/connectors/mysql-cdc-connector.json"
)

$connector = Get-Content $ConfigPath -Raw | ConvertFrom-Json
$body = $connector.config | ConvertTo-Json -Depth 20

Invoke-RestMethod `
    -Method Put `
    -Uri "$ConnectUrl/connectors/$($connector.name)/config" `
    -ContentType "application/json" `
    -Body $body
