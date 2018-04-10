package mappers

import (
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
	c "repodiff/constants"
	e "repodiff/entities"
)

func TestCSVLineToDiffRow(t *testing.T) {
	exampleLine := "2018/02/20,platform/vendor/unbundled_google/packages/Ears,platform/vendor/unbundled_google/packages/Ears,Modified Projects,34,7,25,32,0"
	columns := strings.Split(exampleLine, ",")
	diffRow, err := CSVLineToDiffRow(columns)

	expected := e.DiffRow{
		Date:                 "2018/02/20",
		DownstreamProject:    "platform/vendor/unbundled_google/packages/Ears",
		UpstreamProject:      "platform/vendor/unbundled_google/packages/Ears",
		DiffStatus:           3,
		FilesChanged:         34,
		LineInsertions:       7,
		LineDeletions:        25,
		LineChanges:          32,
		CommitsNotUpstreamed: 0,
	}
	assert.Equal(t, nil, err, "Error should be nil")
	assert.Equal(t, expected, *diffRow, "Entities should be identical")
}

func TestCSVLineToCommitRow(t *testing.T) {
	exampleLine := "2018/02/20,61d5e61b6b6dfbf52d0d433759da964db31cc106,platform/tools/external/gradle,alruiz@google.com,Added Gradle 1.8 to prebuilts."
	columns := strings.Split(exampleLine, ",")
	commitRow, err := CSVLineToCommitRow(columns)

	expected := e.CommitRow{
		Date:              "2018/02/20",
		Commit:            "61d5e61b6b6dfbf52d0d433759da964db31cc106",
		DownstreamProject: "platform/tools/external/gradle",
		Author:            "alruiz@google.com",
		Subject:           "Added Gradle 1.8 to prebuilts.",
	}
	assert.Equal(t, nil, err, "Error should be nil")
	assert.Equal(t, expected, *commitRow, "Entities should be identical")
}

func TestDiffRowsToAggregateChangesOverTime(t *testing.T) {
	rows := DiffRowsToAggregateChangesOverTime(
		[]e.AnalyzedDiffRow{
			makeDiffRow(3, 1, 2),
			makeDiffRow(3, 3, 4),
			makeDiffRow(2, 7, 6),
		},
	)
	cols := rows[0]

	expected := []interface{}{
		"2018022315",
		2,
		11,
		12,
	}
	assert.Equal(t, expected, cols, "Columns should be equal")
}

func makeDiffRow(status, lineChanges, filesChanged int) e.AnalyzedDiffRow {
	return e.AnalyzedDiffRow{
		DiffRow: e.DiffRow{
			DiffStatus:        status,
			LineChanges:       lineChanges,
			FilesChanged:      filesChanged,
			DBInsertTimestamp: 1519427445,
		},
		Type: c.Empty,
	}
}

func TestGetAuthorTechAreaUnknown(t *testing.T) {
	fakeAuthor := "arthur.digby.sellers@google.com"
	techArea := GetAuthorTechArea(fakeAuthor)
	assert.Equal(t, "Unknown", techArea, "Author tech area should be unknown")
}

func TestGetAuthorTechAreaKnown(t *testing.T) {
	fakeAuthor := "jeffrey.lebowski@google.com"
	techArea := GetAuthorTechArea(fakeAuthor)
	assert.Equal(t, "Build", techArea, "Jeffrey Lebowski is on the Build team")
}
