package repositories_test

import (
	e "repodiff/entities"
)

func fakeFixtures() []e.DiffRow {
	return []e.DiffRow{
		e.DiffRow{
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
	}
}

func fakeCommitFixtures() []e.CommitRow {
	return []e.CommitRow{
		e.CommitRow{
			Date:              "2018/02/20",
			Commit:            "61d5e61b6b6dfbf52d0d433759da964db31cc106",
			DownstreamProject: "platform/vendor/unbundled_google/packages/Ears",
			Author:            "slobdell@google.com",
			// Actual commit subject!
			Subject: "Import translations. DO NOT MERGE",
		},
	}
}
