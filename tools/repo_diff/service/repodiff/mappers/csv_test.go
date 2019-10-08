package mappers

import (
	"testing"

	"github.com/stretchr/testify/assert"
	c "repodiff/constants"
	e "repodiff/entities"
)

func TestCommitEntityToCSVRow(t *testing.T) {
	commitRow := e.AnalyzedCommitRow{
		CommitRow: e.CommitRow{
			Date:              "2018/03/19",
			Commit:            "4cc9725c953f57f8abe63b729e26125feac1be4e",
			DownstreamProject: "platform/tools/external/gradle",
			Author:            "jeffrey.lebowski@google.com",
			Subject:           "Take any rug in the house",
		},
		Type: c.Global,
	}
	csvRow := CommitEntityToCSVRow(commitRow)
	expected := []string{
		"\"2018/03/19\"",
		"\"4cc9725c953f57f8abe63b729e26125feac1be4e\"",
		"\"platform/tools/external/gradle\"",
		"\"jeffrey.lebowski@google.com\"",
		"\"Take any rug in the house\"",
		"\"Global\"",
	}
	assert.Equal(t, expected, csvRow, "Strings should be equal")
}

func TestCommitEntityToCSVHeader(t *testing.T) {
	assert.Equal(
		t,
		[]string{
			"Date",
			"Commit",
			"Downstream Project",
			"Author",
			"Subject",
			"Project Type",
		},
		CommitCSVHeader(),
		"Strings should be equal",
	)
}

func TestCommitEntitiesToCSVRows(t *testing.T) {
	commitRow := e.AnalyzedCommitRow{
		CommitRow: e.CommitRow{
			Date:              "2018/03/19",
			Commit:            "4cc9725c953f57f8abe63b729e26125feac1be4e",
			DownstreamProject: "platform/tools/external/gradle",
			Author:            "jeffrey.lebowski@google.com",
			Subject:           "Take any rug in the house",
		},
		Type: c.Global,
	}

	rows := CommitEntitiesToCSVRows(
		[]e.AnalyzedCommitRow{
			commitRow,
			commitRow,
		},
	)
	assert.Equal(t, 2, len(rows), "2 rows should be generated")
}
