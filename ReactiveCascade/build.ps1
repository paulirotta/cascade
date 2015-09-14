param (
    [switch]$display = $false,
    [switch]$test = $false
)

if ($test) {
	./gradlew clean build connectedCheck --stacktrace 
	ii ./cascade/build/reports/androidTests/connected/index.html
} else {
	./gradlew clean build
}

if ($display) {
	ii ./cascade/build/outputs/lint-results.html
}
