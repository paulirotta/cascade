param (
    [switch]$display = $false,
    [switch]$connectedCheck = $false
)

if ($connectedCheck) {
	./gradlew clean build connectedCheck --stacktrace 
	ii ./cascade/build/reports/androidconnectedChecks/connected/index.html
} else {
	./gradlew clean build
}

if ($display) {
	ii ./cascade/build/outputs/lint-results.html
}
