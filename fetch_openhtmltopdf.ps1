<#
fetch_openhtmltopdf.ps1
Downloads a minimal set of JARs from Maven Central required for OpenHTMLToPDF-based HTML->PDF conversion.
Run in PowerShell from the project root: .\fetch_openhtmltopdf.ps1
#>

$ErrorActionPreference = "Stop"
$lib = Join-Path $PSScriptRoot 'lib'
if (-not (Test-Path $lib)) { New-Item -ItemType Directory -Path $lib | Out-Null }

$artifacts = @(
  "https://repo1.maven.org/maven2/com/openhtmltopdf/openhtmltopdf-core/1.0.10/openhtmltopdf-core-1.0.10.jar",
  "https://repo1.maven.org/maven2/com/openhtmltopdf/openhtmltopdf-pdfbox/1.0.10/openhtmltopdf-pdfbox-1.0.10.jar",
  "https://repo1.maven.org/maven2/org/apache/pdfbox/pdfbox/2.0.29/pdfbox-2.0.29.jar",
  "https://repo1.maven.org/maven2/org/apache/pdfbox/fontbox/2.0.29/fontbox-2.0.29.jar",
  "https://repo1.maven.org/maven2/commons-io/commons-io/2.11.0/commons-io-2.11.0.jar",
  "https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar"
)

foreach ($url in $artifacts) {
  try {
    $file = Join-Path $lib ([IO.Path]::GetFileName($url))
    if (Test-Path $file) {
      Write-Host "Already exists: $file"
      continue
    }
    Write-Host "Downloading $url ..."
    Invoke-WebRequest -Uri $url -OutFile $file -UseBasicParsing -ErrorAction Stop
    Write-Host "Saved to: $file"
  } catch {
    Write-Warning "Failed to download $url : $_"
  }
}
Write-Host "Download complete. JARs are in: $lib"
Write-Host "Now compile and run with: javac -cp 'lib/*' --module-path 'C:\path\to\javafx-sdk\lib' --add-modules javafx.controls,javafx.web -d out *.java"
Write-Host "Then run: java --module-path 'C:\path\to\javafx-sdk\lib' --add-modules javafx.controls,javafx.web -cp 'out;lib/*' MainApp"