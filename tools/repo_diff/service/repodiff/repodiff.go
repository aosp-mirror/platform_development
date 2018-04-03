package main

import (
	"fmt"
	"repodiff/constants"
	"repodiff/controllers"
	e "repodiff/entities"
	"repodiff/handlers"
	"repodiff/persistence/filesystem"
)

const configFile = "config.json"

func main() {
	appConfig, err := loadConfig()
	if err != nil {
		panic(formattedError(err))
	}
	statusChannel := make(chan e.StatusMessage)
	go handlers.StartHTTP(appConfig.Port, statusChannel)
	go run(appConfig, statusChannel)
	select {}
}

func run(appConfig e.ApplicationConfig, statusChannel chan e.StatusMessage) {
	statusChannel <- e.StatusMessage{
		JobStatus: constants.JobStatusRunning,
	}
	err := controllers.ExecuteDifferentials(appConfig)
	if err != nil {
		statusChannel <- e.StatusMessage{
			JobStatus: constants.JobStatusFailed,
			Meta:      formattedError(err),
		}
		fmt.Println(formattedError(err))
		return
	}
	err = controllers.DenormalizeData(appConfig)
	if err != nil {
		statusChannel <- e.StatusMessage{
			JobStatus: constants.JobStatusFailed,
			Meta:      formattedError(err),
		}
		fmt.Println(formattedError(err))
		return
	}
	statusChannel <- e.StatusMessage{
		JobStatus: constants.JobStatusComplete,
	}
	fmt.Println("Finished")
}

func loadConfig() (e.ApplicationConfig, error) {
	var appConfig e.ApplicationConfig
	err := filesystem.ReadFileAsJson(configFile, &appConfig)
	if err != nil {
		return appConfig, err
	}
	return appConfig, nil
}

func formattedError(err error) string {
	return fmt.Sprintf("%+v", err)
}
