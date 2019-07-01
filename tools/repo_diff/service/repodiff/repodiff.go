package main

import (
	"fmt"
	"repodiff/constants"
	"repodiff/controllers"
	ent "repodiff/entities"
	"repodiff/handlers"
	"repodiff/persistence/filesystem"
)

const configFile = "config.json"

type controllerFunc func(ent.ApplicationConfig) error

func main() {
	appConfig, err := loadConfig()
	if err != nil {
		panic(formattedError(err))
	}
	statusChannel := make(chan ent.StatusMessage)
	go handlers.StartHTTP(appConfig.Port, statusChannel)
	go run(appConfig, statusChannel)
	select {}
}

func run(appConfig ent.ApplicationConfig, statusChannel chan ent.StatusMessage) {
	statusChannel <- ent.StatusMessage{
		JobStatus: constants.JobStatusRunning,
	}

	for _, controllerFn := range getEnabledControllers() {
		if err := controllerFn(appConfig); err != nil {
			topLevelErrorHandle(err, statusChannel)
			return
		}
	}
	statusChannel <- ent.StatusMessage{
		JobStatus: constants.JobStatusComplete,
	}
}

func getEnabledControllers() []controllerFunc {
	enabled := getEnabledOperations()
	return []controllerFunc{
		disabledFnNullified(controllers.ExecuteDifferentials, enabled.Diff),
		disabledFnNullified(controllers.DenormalizeData, enabled.Denorm),
		disabledFnNullified(controllers.GenerateCommitReport, enabled.Report),
	}
}

func disabledFnNullified(original controllerFunc, enabled bool) controllerFunc {
	if enabled {
		return original
	}
	return func(ent.ApplicationConfig) error {
		return nil
	}
}

func topLevelErrorHandle(err error, statusChannel chan ent.StatusMessage) {
	statusChannel <- ent.StatusMessage{
		JobStatus: constants.JobStatusFailed,
		Meta:      formattedError(err),
	}
	fmt.Println(formattedError(err))
}

func loadConfig() (ent.ApplicationConfig, error) {
	var appConfig ent.ApplicationConfig
	err := filesystem.ReadFileAsJson(configFile, &appConfig)
	if err != nil {
		return appConfig, err
	}
	return appConfig, nil
}

func formattedError(err error) string {
	return fmt.Sprintf("%+v", err)
}
