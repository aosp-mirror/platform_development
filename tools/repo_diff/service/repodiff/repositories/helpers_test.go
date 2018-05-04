package repositories_test

import (
	"fmt"
	cst "repodiff/constants"
	ent "repodiff/entities"
	repoSQL "repodiff/persistence/sql"
)

func fakeFixtures() []ent.AnalyzedDiffRow {
	return []ent.AnalyzedDiffRow{
		ent.AnalyzedDiffRow{
			DiffRow: ent.DiffRow{
				Date:                 "2018/02/20",
				DownstreamProject:    "platform/vendor/unbundled_google/packages/Ears",
				UpstreamProject:      "platform/vendor/unbundled_google/packages/Ears",
				DiffStatus:           3,
				FilesChanged:         34,
				LineInsertions:       8,
				LineDeletions:        25,
				LineChanges:          32,
				CommitsNotUpstreamed: 0,
			},
			Type: cst.Empty,
		},
	}
}

func fakeCommitFixtures() []ent.AnalyzedCommitRow {
	return []ent.AnalyzedCommitRow{
		ent.AnalyzedCommitRow{
			CommitRow: ent.CommitRow{
				Date:              "2018/02/20",
				Commit:            "61d5e61b6b6dfbf52d0d433759da964db31cc106",
				DownstreamProject: "platform/vendor/unbundled_google/packages/Ears",
				Author:            "slobdell@google.com",
				// Actual commit subject!
				Subject: "Import translations. DO NOT MERGE",
			},
			Type: cst.Empty,
		},
	}
}

func clearTableBeforeAfterTest(tableName string) func() {
	clearTable(tableName)
	return func() {
		clearTable(tableName)
	}
}

func clearTable(tableName string) {
	db, _ := repoSQL.GetDBConnectionPool()
	db.Exec(
		fmt.Sprintf("TRUNCATE TABLE %s", tableName),
	)
}
