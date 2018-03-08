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
