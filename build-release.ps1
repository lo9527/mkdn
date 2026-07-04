# build-release.ps1 — 主人在本地 release 打包用（不入仓）
#
# 用法（在 PowerShell 里）:
#   .\build-release.ps1 -StorePassword 'xxx' -KeyPassword 'xxx'
#
# 为什么不写死密码：本文件将提交到公开 GitHub 仓库，
# 真实密码通过命令行参数 / 环境变量传入，避免泄露

param(
    [string]$StoreFile = 'mkdn-release.jks',
    [string]$StorePassword = $env:MKDN_RELEASE_STORE_PASSWORD,
    [string]$KeyAlias = $env:MKDN_RELEASE_KEY_ALIAS,
    [string]$KeyPassword = $env:MKDN_RELEASE_KEY_PASSWORD
)

if (-not $StorePassword) {
    Write-Host '❌ 未提供 StorePassword，请通过 -StorePassword 或环境变量 MKDN_RELEASE_STORE_PASSWORD 传入' -ForegroundColor Red
    exit 1
}
if (-not $KeyPassword) {
    Write-Host '❌ 未提供 KeyPassword，请通过 -KeyPassword 或环境变量 MKDN_RELEASE_KEY_PASSWORD 传入' -ForegroundColor Red
    exit 1
}
if (-not $KeyAlias) {
    $KeyAlias = 'mkdn'
}

# === 设置签名环境变量 ===
$env:MKDN_RELEASE_STORE_FILE = $StoreFile
$env:MKDN_RELEASE_STORE_PASSWORD = $StorePassword
$env:MKDN_RELEASE_KEY_ALIAS = $KeyAlias
$env:MKDN_RELEASE_KEY_PASSWORD = $KeyPassword

# === Java 环境（主人原工作流） ===
$env:JAVA_HOME = 'E:\AI\Android studio\jbr'
$env:Path = 'E:\AI\Android studio\jbr\bin;' + $env:Path

# === 进入项目根 ===
Set-Location $PSScriptRoot

# === 调用 gradle ===
& '.\gradlew.bat' assembleRelease

# === 检查结果 ===
if ($LASTEXITCODE -eq 0) {
    Write-Host ''
    Write-Host '✅ BUILD SUCCESSFUL' -ForegroundColor Green
    Write-Host 'APK: app\build\outputs\apk\release\app-release.apk' -ForegroundColor Cyan
} else {
    Write-Host ''
    Write-Host '❌ BUILD FAILED' -ForegroundColor Red
    exit $LASTEXITCODE
}
