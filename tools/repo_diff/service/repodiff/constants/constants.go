package constants

import (
	"fmt"

	"github.com/pkg/errors"
	"github.com/satori/go.uuid"
)

const (
	StatusIntact         = 1
	StatusDownstreamOnly = 2
	StatusModified       = 3
	StatusUpstream       = 4
	StatusForked         = 5
)

const (
	JobStatusNotStarted = "Not Started"
	JobStatusRunning    = "Running"
	JobStatusFailed     = "Failed"
	JobStatusComplete   = "Completed"
)

var StatusToDisplay = map[int]string{
	StatusIntact:         "Intact Projects",
	StatusDownstreamOnly: "Downstream Only Projects",
	StatusModified:       "Modified Projects",
	StatusUpstream:       "Upstream Only Projects",
	StatusForked:         "Forked Projects",
}

type ProjectType int

var Empty ProjectType = 0
var DifferentialSpecific ProjectType = 1
var Global ProjectType = 2

var ProjectTypeToDisplay = map[ProjectType]string{
	Empty:                "Empty",
	DifferentialSpecific: "Differential Specific",
	Global:               "Global",
}

var displayToStatus map[string]int

func makeDisplayToStatus() map[string]int {
	return invertMap(StatusToDisplay)
}

func GetStatusEnum(displayStatus string) (int, error) {
	if displayToStatus == nil {
		displayToStatus = makeDisplayToStatus()
	}
	val, ok := displayToStatus[displayStatus]
	if !ok {
		return 0, errors.New(
			fmt.Sprintf("No matching enum for %s", displayStatus),
		)
	}
	return val, nil
}

func invertMap(m map[int]string) map[string]int {
	inverted := make(map[string]int, len(m))
	for k := range StatusToDisplay {
		v := StatusToDisplay[k]
		inverted[v] = k
	}
	return inverted
}

func NullUUID() uuid.UUID {
	emptyBytes := make([]byte, 16)
	u, _ := uuid.FromBytes(emptyBytes)
	return u
}
