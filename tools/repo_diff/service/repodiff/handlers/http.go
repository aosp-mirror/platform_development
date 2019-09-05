package handlers

import (
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"time"

	"github.com/pkg/errors"

	"repodiff/constants"
	e "repodiff/entities"
)

var globalJobStatus = constants.JobStatusNotStarted
var globalMeta string
var globalStartTime time.Time

type healthResponse struct {
	ApplicationStatus string `json:"application_status"`
	JobStatus         string `json:"job_status"`
	Meta              string `json:"meta"`
	ElapsedTime       string `json:"elapsed_time"`
}

func writeJsonResponse(writer http.ResponseWriter, entity interface{}) {
	serialized, err := json.MarshalIndent(entity, "", "    ")
	if err != nil {
		log.Fatal(err)
	}
	writer.Header().Set("Content-Type", "application/json")
	writer.Write(serialized)
}

func handleHealth(writer http.ResponseWriter, request *http.Request) {
	switch request.Method {
	case "GET":
		writeJsonResponse(
			writer,
			healthResponse{
				ApplicationStatus: "ok",
				JobStatus:         globalJobStatus,
				Meta:              globalMeta,
				ElapsedTime:       fmt.Sprintf("%s", time.Now().Sub(globalStartTime)),
			},
		)
	}
}

func listenForStatusChanges(statusChannel chan e.StatusMessage) {
	for {
		m := <-statusChannel
		globalJobStatus = m.JobStatus
		globalMeta = m.Meta
	}
}

func StartHTTP(servePort int, statusChannel chan e.StatusMessage) error {
	globalStartTime = time.Now()
	go listenForStatusChanges(statusChannel)
	http.HandleFunc("/health", handleHealth)
	return errors.Wrap(
		http.ListenAndServe(
			fmt.Sprintf(":%d", servePort),
			nil,
		),
		"Error starting web server",
	)
}
