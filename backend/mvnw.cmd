<# : batch portion
@REM Apache Maven Wrapper startup batch script, compatible with Maven Wrapper 3.3.4 only-script mode.
@REM Licensed under the Apache License, Version 2.0.
@IF "%__MVNW_ARG0_NAME__%"=="" (SET __MVNW_ARG0_NAME__=%~nx0)
@SET __MVNW_CMD__=
@SET __MVNW_PSMODULEP_SAVE=%PSModulePath%
@SET PSModulePath=
@FOR /F "usebackq tokens=1* delims==" %%A IN (`powershell -noprofile "& {$scriptDir='%~dp0'; $script='%__MVNW_ARG0_NAME__%'; icm -ScriptBlock ([Scriptblock]::Create((Get-Content -Raw '%~f0'))) -NoNewScope}"`) DO @(
IF "%%A"=="MVN_CMD" (set __MVNW_CMD__=%%B) ELSE IF "%%B"=="" (echo %%A) ELSE (echo %%A=%%B)
)
@SET PSModulePath=%__MVNW_PSMODULEP_SAVE%
@SET __MVNW_PSMODULEP_SAVE=
@SET __MVNW_ARG0_NAME__=
@SET MVNW_USERNAME=
@SET MVNW_PASSWORD=
@IF NOT "%__MVNW_CMD__%"=="" ("%__MVNW_CMD__%" %*)
@echo Cannot start maven from wrapper >&2 && exit /b 1
@GOTO :EOF
: end batch / begin powershell #>

$ErrorActionPreference = "Stop"
if ($env:MVNW_VERBOSE -eq "true") { $VerbosePreference = "Continue" }

$properties = Get-Content -Raw "$scriptDir/.mvn/wrapper/maven-wrapper.properties" | ConvertFrom-StringData
$distributionUrl = $properties.distributionUrl
if (!$distributionUrl) { Write-Error "cannot read distributionUrl property in $scriptDir/.mvn/wrapper/maven-wrapper.properties" }
if ($env:MVNW_REPOURL) {
    $distributionUrl = "$env:MVNW_REPOURL/org/apache/maven/$($distributionUrl -replace '^.*/org/apache/maven/','')"
}

$distributionUrlName = $distributionUrl -replace '^.*/',''
$distributionUrlNameMain = $distributionUrlName -replace '\.[^.]*$','' -replace '-bin$',''
$MAVEN_M2_PATH = if ($env:MAVEN_USER_HOME) { $env:MAVEN_USER_HOME } else { "$HOME/.m2" }
New-Item -Path $MAVEN_M2_PATH -ItemType Directory -Force | Out-Null
$MAVEN_WRAPPER_DISTS = "$MAVEN_M2_PATH/wrapper/dists"
$MAVEN_HOME_PARENT = "$MAVEN_WRAPPER_DISTS/$distributionUrlNameMain"
$MAVEN_HOME_NAME = ([System.Security.Cryptography.SHA256]::Create().ComputeHash([byte[]][char[]]$distributionUrl) | ForEach-Object {$_.ToString("x2")}) -join ''
$MAVEN_HOME = "$MAVEN_HOME_PARENT/$MAVEN_HOME_NAME"
$MVN_CMD = "mvn.cmd"

if (Test-Path -Path "$MAVEN_HOME/bin/$MVN_CMD" -PathType Leaf) {
    Write-Output "MVN_CMD=$MAVEN_HOME/bin/$MVN_CMD"
    exit 0
}

if (!$distributionUrlNameMain -or ($distributionUrlName -eq $distributionUrlNameMain)) {
    Write-Error "distributionUrl is not valid, must end with *-bin.zip, but found $distributionUrl"
}

$TMP_DOWNLOAD_DIR_HOLDER = New-TemporaryFile
$TMP_DOWNLOAD_DIR = New-Item -ItemType Directory -Path "$TMP_DOWNLOAD_DIR_HOLDER.dir"
$TMP_DOWNLOAD_DIR_HOLDER.Delete() | Out-Null
trap {
    if ($TMP_DOWNLOAD_DIR.Exists) {
        try { Remove-Item $TMP_DOWNLOAD_DIR -Recurse -Force | Out-Null }
        catch { Write-Warning "Cannot remove $TMP_DOWNLOAD_DIR" }
    }
}
New-Item -ItemType Directory -Path "$MAVEN_HOME_PARENT" -Force | Out-Null

$webclient = New-Object System.Net.WebClient
if ($env:MVNW_USERNAME -and $env:MVNW_PASSWORD) {
    $webclient.Credentials = New-Object System.Net.NetworkCredential($env:MVNW_USERNAME, $env:MVNW_PASSWORD)
}
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$webclient.DownloadFile($distributionUrl, "$TMP_DOWNLOAD_DIR/$distributionUrlName") | Out-Null

if ($properties.distributionSha256Sum) {
    Import-Module $PSHOME\Modules\Microsoft.PowerShell.Utility -Function Get-FileHash
    if ((Get-FileHash "$TMP_DOWNLOAD_DIR/$distributionUrlName" -Algorithm SHA256).Hash.ToLower() -ne $properties.distributionSha256Sum) {
        Write-Error "Maven distribution SHA-256 validation failed"
    }
}

Expand-Archive "$TMP_DOWNLOAD_DIR/$distributionUrlName" -DestinationPath "$TMP_DOWNLOAD_DIR" | Out-Null
$expectedPath = Join-Path "$TMP_DOWNLOAD_DIR" "$distributionUrlNameMain"
$actualDistributionDir = $null
if (Test-Path -Path "$expectedPath/bin/$MVN_CMD" -PathType Leaf) {
    $actualDistributionDir = $distributionUrlNameMain
} else {
    Get-ChildItem -Path "$TMP_DOWNLOAD_DIR" -Directory | ForEach-Object {
        if (!$actualDistributionDir -and (Test-Path -Path (Join-Path $_.FullName "bin/$MVN_CMD") -PathType Leaf)) {
            $actualDistributionDir = $_.Name
        }
    }
}
if (!$actualDistributionDir) { Write-Error "Could not find Maven distribution directory in extracted archive" }
Rename-Item -Path "$TMP_DOWNLOAD_DIR/$actualDistributionDir" -NewName $MAVEN_HOME_NAME | Out-Null
try {
    Move-Item -Path "$TMP_DOWNLOAD_DIR/$MAVEN_HOME_NAME" -Destination $MAVEN_HOME_PARENT | Out-Null
} catch {
    if (!(Test-Path -Path "$MAVEN_HOME" -PathType Container)) { throw }
} finally {
    try { Remove-Item $TMP_DOWNLOAD_DIR -Recurse -Force | Out-Null } catch { }
}
Write-Output "MVN_CMD=$MAVEN_HOME/bin/$MVN_CMD"
