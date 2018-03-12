package main

import (
	"fmt"
	"repodiff/controllers"
	"repodiff/entities"
	"repodiff/persistence/filesystem"
)

func main() {
	appConfig, err := loadConfig()
	if err != nil {
		panic(formattedError(err))
	}

	err = controllers.ExecuteDifferentials(appConfig)
	if err != nil {
		panic(formattedError(err))
	}
	err = controllers.DenormalizeData()
	if err != nil {
		panic(formattedError(err))
	}
}

func loadConfig() (entities.ApplicationConfig, error) {
	var appConfig entities.ApplicationConfig
	err := filesystem.ReadFileAsJson("config.json", &appConfig)
	if err != nil {
		return appConfig, err
	}
	return appConfig, nil
}

func formattedError(err error) string {
	return fmt.Sprintf("%+v", err)
}
