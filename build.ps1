param (
    [switch]$clean = $false,
    [switch]$lint = $false,
    [switch]$test = $false,
    [switch]$connectedCheck = $false,
    [switch]$javadoc = $false
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

if ($connectedCheck) {
	./gradlew connectedCheck --info
	ii ./cascade/build/reports/androidTests/connected/index.html
}

if ($javadoc) {
	./gradlew generateReleaseJavadoc
	ii ./cascade/build/docs/
	ii ./cascade/build/docs/javadoc/index.html
	""
	"Add JavaDoc manually to gh-pages branch"	
}
