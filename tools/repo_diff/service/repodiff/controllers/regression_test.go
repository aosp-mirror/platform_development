package controllers

import (
	"testing"

	"github.com/stretchr/testify/assert"

	cst "repodiff/constants"
	ent "repodiff/entities"
	"repodiff/repositories"
)

func TestRegressionIncorrectStringValue(t *testing.T) {
	commitRows, _ := CSVFileToCommitRows("testdata/commit.csv")
	analyzed := make([]ent.AnalyzedCommitRow, len(commitRows))
	for i, row := range commitRows {
		analyzed[i] = ent.AnalyzedCommitRow{
			CommitRow: row,
			Type:      cst.Empty,
		}
	}

	c, _ := repositories.NewCommitRepository(
		ent.MappedDiffTarget{
			UpstreamTarget:   1,
			DownstreamTarget: 2,
		},
	)
	err := c.InsertCommitRows(analyzed)
	assert.Equal(t, nil, err, "Error should be nil")
}
