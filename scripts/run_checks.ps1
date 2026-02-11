# Run basic checks for the project
Write-Host "Running Gradle checks..."
$log = "build_checks.log"

if (Test-Path './gradlew') {
    Write-Host "Using gradlew"
    & .\gradlew clean | Tee-Object -FilePath $log -Append
    & .\gradlew test | Tee-Object -FilePath $log -Append
    & .\gradlew assembleDebug | Tee-Object -FilePath $log -Append
    Write-Host "Finished. See $log for details."
} else {
    Write-Host "gradlew not found. Ensure you're in the project root and gradle wrapper exists."
}
