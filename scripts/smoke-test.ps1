$ErrorActionPreference = "Stop"
$baseUrl = if ($env:GATEWAY_URL) { $env:GATEWAY_URL.TrimEnd('/') } else { "http://localhost:8080" }
$elasticUrl = if ($env:ELASTICSEARCH_URL) { $env:ELASTICSEARCH_URL.TrimEnd('/') } else { "http://localhost:9200" }

Write-Host "1/6 - Checking Gateway health..."
$health = Invoke-RestMethod -Method Get -Uri "$baseUrl/actuator/health"
if ($health.status -ne "UP") { throw "Gateway health is not UP: $($health.status)" }

Write-Host "2/6 - Checking Prometheus metrics endpoint..."
$metrics = Invoke-WebRequest -Method Get -Uri "$baseUrl/actuator/prometheus"
if ($metrics.StatusCode -ne 200 -or $metrics.Content -notmatch "jvm_memory") {
    throw "Prometheus endpoint did not return expected metrics"
}

$runId = [guid]::NewGuid().ToString("N")
$email = if ($env:TEST_USER_EMAIL) { $env:TEST_USER_EMAIL } else { "gateway-smoke-$runId@example.com" }
$password = if ($env:TEST_USER_PASSWORD) { $env:TEST_USER_PASSWORD } else { "GatewaySmoke@2026" }

Write-Host "3/6 - Creating test account..."
$signupBody = @{ name = "Gateway Smoke Test"; email = $email; password = $password } | ConvertTo-Json
try {
    Invoke-RestMethod -Method Post -Uri "$baseUrl/user/signup" -ContentType "application/json" -Body $signupBody | Out-Null
} catch {
    $statusCode = $null
    if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
        $statusCode = [int]$_.Exception.Response.StatusCode
    }
    if ($statusCode -ne 409) { throw }
}

Write-Host "4/6 - Logging in and checking protected User API..."
$loginBody = @{ email = $email; password = $password } | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -Uri "$baseUrl/user/login" -ContentType "application/json" -Body $loginBody
$token = $login.accessToken
if (-not $token) { throw "Login did not return accessToken" }
$headers = @{ Authorization = "Bearer $token" }
$user = Invoke-RestMethod -Method Get -Uri "$baseUrl/user" -Headers $headers
if ($user.email -ne $email) { throw "Authenticated profile did not return the test user's email" }

Write-Host "5/6 - Checking Order create/read/delete through Gateway..."
$orderBody = @{ productName = "Gateway Smoke Item"; quantity = 1; price = 19.99 } | ConvertTo-Json
$order = Invoke-RestMethod -Method Post -Uri "$baseUrl/orders" -Headers $headers -ContentType "application/json" -Body $orderBody
if (-not $order.id) { throw "Order creation did not return an id" }
$loadedOrder = Invoke-RestMethod -Method Get -Uri "$baseUrl/orders/$($order.id)" -Headers $headers
if ($loadedOrder.id -ne $order.id) { throw "Created order could not be read back" }
Invoke-RestMethod -Method Delete -Uri "$baseUrl/orders/$($order.id)" -Headers $headers | Out-Null

Write-Host "6/6 - Waiting for Kafka consumer to index Gateway request events..."
$indexed = $false
for ($attempt = 1; $attempt -le 20; $attempt++) {
    try {
        $search = Invoke-RestMethod -Method Get -Uri "$elasticUrl/gateway-logs/_search?size=1"
        $total = if ($search.hits.total -is [System.Collections.IDictionary]) { $search.hits.total.value } else { $search.hits.total }
        if ($total -gt 0) { $indexed = $true; break }
    } catch { }
    Start-Sleep -Seconds 2
}
if (-not $indexed) { throw "No Gateway event was found in Elasticsearch within 40 seconds" }

Write-Host "Smoke test passed: Gateway, User, Order, Prometheus, Kafka and Elasticsearch." -ForegroundColor Green
Write-Host "Test account: $email"
