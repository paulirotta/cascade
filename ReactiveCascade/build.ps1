param (
    [switch]$lint = $false,
    [switch]$test = $false,
    [switch]$connectedTest = $false
)

./gradlew clean build

if ($test) {
	./gradlew test --stacktrace 	
}

if ($connectedTest) {
	./gradlew connectedCheck --stacktrace 
}

if ($lint) {
	ii ./cascade/build/outputs/lint-results.html
}

if ($test) {
	ii ./cascade/build/reports/tests/debug/index.html
	ii ./cascade/build/reports/tests/release/index.html
}

if ($connectedTest) {
	ii ./cascade/build/reports/androidTests/connected/index.html
}

