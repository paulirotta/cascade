param (
    [switch]$lint = $false,
    [switch]$test = $false,
    [switch]$connectedTest = $false
)

if ($test) {
	if ($connectedTest) {
		./gradlew clean build test connectedCheck --stacktrace 
	} else {
		./gradlew clean build test --stacktrace 					
	}
} elseif ($connectedTest) {
	./gradlew clean build connectedCheck --stacktrace 
} else {
	./gradlew clean build
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

