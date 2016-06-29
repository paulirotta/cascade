param (
    [switch]$clean = $false,
    [switch]$lint = $false,
    [switch]$test = $false,
    [switch]$connectedTest = $false
)

if ($clean) {
	./gradlew clean
	if (-Not $?) {
		exit
	}
}

if ($lint) {
	./gradlew lint
	if ($?) {
		ii ./cascade/build/outputs/lint-results-debug.html
	} else {
		exit
	}
}

if ($test) {
	./gradlew test --info
	if (-Not $?) {
		ii ./cascade/build/reports/tests/debug/index.html
		exit
	}
	ii ./cascade/build/reports/tests/debug/index.html
}

if ($connectedTest) {
	./gradlew connectedCheck --info
	ii ./cascade/build/reports/androidTests/connected/index.html
}
