# Starts the MySQL server that AgriDirect uses on Windows.
# - If MySQL is already running on port 3306 (e.g. an installed MySQL service), it does nothing.
# - Otherwise it uses the portable MySQL in the project's "mysql" folder, downloading it the
#   first time (official ZIP from cdn.mysql.com, about 300 MB, no installer and no admin needed),
#   initialises it with user "root" and an empty password, and starts it in the background.
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

$project = Split-Path -Parent $PSScriptRoot
$mysql   = Join-Path $project 'mysql'
$mysqld  = Join-Path $mysql 'bin\mysqld.exe'
$data    = Join-Path $mysql 'data'
$ini     = Join-Path $mysql 'my.ini'
$port    = 3306

function Test-MySqlPort {
    try {
        $c = New-Object Net.Sockets.TcpClient
        $c.Connect('127.0.0.1', $port)
        $c.Close()
        return $true
    } catch {
        return $false
    }
}

if (Test-MySqlPort) {
    Write-Host "MySQL is already running on port $port."
    exit 0
}

# ---------- 1. download portable MySQL (first time only) ----------
if (-not (Test-Path $mysqld)) {
    Write-Host ""
    Write-Host "MySQL is not installed. Downloading the official portable MySQL (about 300 MB)."
    Write-Host "This happens only once. Please wait..."
    $versions = '8.4.12', '8.4.11', '8.4.10', '8.4.9', '8.4.8', '8.4.7', '8.4.6', '8.4.5', '8.4.4', '8.4.3'
    $urls = @()
    foreach ($v in $versions) {
        $urls += "https://cdn.mysql.com/Downloads/MySQL-8.4/mysql-$v-winx64.zip"
        $urls += "https://cdn.mysql.com/archives/mysql-8.4/mysql-$v-winx64.zip"
    }
    $zip = Join-Path $env:TEMP 'agridirect-mysql.zip'
    $ok = $false
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    foreach ($u in $urls) {
        try {
            Invoke-WebRequest -Uri $u -OutFile $zip -UseBasicParsing
            if ((Get-Item $zip).Length -gt 100MB) {
                Write-Host "Downloaded $u"
                $ok = $true
                break
            }
        } catch {
            # try the next version
        }
    }
    if (-not $ok) {
        Write-Host ""
        Write-Host "Could not download MySQL automatically (no internet, or the download site changed)."
        Write-Host "Install MySQL Server yourself from https://dev.mysql.com/downloads/installer/ and run run.bat again."
        exit 1
    }

    Write-Host "Unpacking..."
    $tmp = Join-Path $env:TEMP 'agridirect-mysql-unzip'
    if (Test-Path $tmp) { Remove-Item -Recurse -Force $tmp }
    New-Item -ItemType Directory -Path $tmp | Out-Null
    & tar.exe -xf $zip -C $tmp
    if ($LASTEXITCODE -ne 0) { Expand-Archive -Path $zip -DestinationPath $tmp -Force }
    $inner = Get-ChildItem $tmp -Directory | Select-Object -First 1
    if (Test-Path $mysql) { Remove-Item -Recurse -Force $mysql }
    Move-Item $inner.FullName $mysql
    Remove-Item $zip -Force
    Remove-Item -Recurse -Force $tmp
    Write-Host "MySQL unpacked to $mysql"
}

# ---------- 2. config file ----------
$base = ($mysql -replace '\\', '/')
@"
[mysqld]
basedir="$base"
datadir="$base/data"
port=$port
bind-address=127.0.0.1
character-set-server=utf8mb4
"@ | Set-Content -Path $ini -Encoding ASCII

# ---------- 3. initialise the data folder (first time only) ----------
if (-not (Test-Path $data)) {
    Write-Host "Setting up the MySQL data folder (user root, no password)..."
    & $mysqld "--defaults-file=$ini" --initialize-insecure --console
    if ($LASTEXITCODE -ne 0) {
        if ($LASTEXITCODE -eq -1073741515) {
            Write-Host ""
            Write-Host "MySQL needs the Microsoft Visual C++ Redistributable."
            Write-Host "Install it from https://aka.ms/vs/17/release/vc_redist.x64.exe and run run.bat again."
        } else {
            Write-Host "MySQL could not set up its data folder (exit code $LASTEXITCODE)."
        }
        if (Test-Path $data) { Remove-Item -Recurse -Force $data }
        exit 1
    }
}

# ---------- 4. start the server in the background ----------
Write-Host "Starting MySQL..."
Start-Process -FilePath $mysqld -ArgumentList "--defaults-file=`"$ini`"" -WindowStyle Hidden
for ($i = 0; $i -lt 60; $i++) {
    if (Test-MySqlPort) {
        Write-Host "MySQL is running on port $port."
        exit 0
    }
    Start-Sleep -Seconds 1
}
Write-Host "MySQL did not start. See the .err file in $data for details."
exit 1
