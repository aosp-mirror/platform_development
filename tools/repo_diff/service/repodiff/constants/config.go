package constants

import (
	"os"
)

func GetConfigVar(key string) string {
	if os.Getenv("ROLE") == "prod" {
		key += "_PROD"
	} else if os.Getenv("ROLE") == "dev" {
		key += "_DEV"
	} else {
		panic("Application has not been executed correctly. Specify environment variable 'ROLE' as either 'dev' or 'prod'")
	}
	return os.Getenv(key)
}
