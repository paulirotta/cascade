param (
    [switch]$display = $false,
    [switch]$test = $false
)

if ($test) {
	./gradlew clean build connectedCheck
} else {
	./gradlew clean build
}

if ($display) {
	ii ./cascade/build/outputs/lint-results.html
	ii ./cascade/build/reports/androidTests/connected/index.html
}
