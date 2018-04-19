package mappers

import (
	"fmt"
	c "repodiff/constants"
	e "repodiff/entities"
)

func CommitCSVHeader() []string {
	return []string{
		"Date",
		"Commit",
		"Downstream Project",
		"Author",
		"Subject",
		"Project Type",
	}
}

func CommitEntityToCSVRow(a e.AnalyzedCommitRow) []string {
	return quoteAll(
		[]string{
			a.Date,
			a.Commit,
			a.DownstreamProject,
			a.Author,
			a.Subject,
			c.ProjectTypeToDisplay[a.Type],
		},
	)
}

func CommitEntitiesToCSVRows(commits []e.AnalyzedCommitRow) [][]string {
	rowsOfCols := make([][]string, len(commits))
	for i, commit := range commits {
		cols := CommitEntityToCSVRow(commit)
		rowsOfCols[i] = cols
	}
	return rowsOfCols
}

func quoteAll(s []string) []string {
	copied := make([]string, len(s))
	for i, val := range s {
		copied[i] = fmt.Sprintf("%q", val)
	}
	return copied
}
