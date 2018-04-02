package mappers

import (
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
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
		[]e.DiffRow{
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

func makeDiffRow(status, lineChanges, filesChanged int) e.DiffRow {
	return e.DiffRow{
		DiffStatus:        status,
		LineChanges:       lineChanges,
		FilesChanged:      filesChanged,
		DBInsertTimestamp: 1519427445,
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

func TestCommitRowsToTopCommitter(t *testing.T) {
	fakeCommitRows := []e.CommitRow{
		e.CommitRow{
			Date:              "2018/03/20",
			Commit:            "540eecd728a407e4b31a38f4ea9416dea7d05c0c",
			DownstreamProject: "platform/tools/external/gradle",
			Author:            "jeffrey.lebowski@google.com",
			Subject:           "Hand off the briefcase",
		},
		e.CommitRow{
			Date:              "2018/03/19",
			Commit:            "ea999655a8af4b7d6a8033d1c864ca87617d0ede",
			DownstreamProject: "platform/tools/external/gradle",
			Author:            "brandt@google.com",
			Subject:           "We Just Don't Know",
		},
		e.CommitRow{
			Date:              "2018/03/19",
			Commit:            "4cc9725c953f57f8abe63b729e26125feac1be4e",
			DownstreamProject: "platform/tools/external/gradle",
			Author:            "jeffrey.lebowski@google.com",
			Subject:           "Take any rug in the house",
		},
	}
	aggregated := CommitRowsToTopCommitter(fakeCommitRows)
	expected := [][]interface{}{
		[]interface{}{
			1,
			"jeffrey.lebowski@google.com",
			2,
			0,
			"Build",
		},
		[]interface{}{
			2,
			"brandt@google.com",
			1,
			0,
			"Unknown",
		},
	}
	assert.Equal(t, expected, aggregated, "Rows should be equal")
}
