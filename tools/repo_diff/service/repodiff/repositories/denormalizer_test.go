package repositories_test

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"

	c "repodiff/constants"
	e "repodiff/entities"
	repoSQL "repodiff/persistence/sql"
	"repodiff/repositories"
)

const arbitraryTimestamp = e.RepoTimestamp(1525978906)

var fakeTarget = e.DiffTarget{
	Upstream: e.Project{
		URL:    "https://keystone-qcom.googlesource.com/platform/manifest",
		Branch: "p-fs-release",
	},
	Downstream: e.Project{
		URL:    "https://keystone-qcom.googlesource.com/platform/manifest",
		Branch: "p-keystone-qcom",
	},
}
var fakeMappedTarget = e.MappedDiffTarget{
	UpstreamTarget:   1,
	DownstreamTarget: 2,
}

func init() {
	clearTable("denormalized_view_recent_project")
	clearTable("denormalized_view_changes_over_time")
}

func getRowCountAtTable(tableName string) int {
	db, _ := repoSQL.GetDBConnectionPool()
	var count int
	db.QueryRow(
		fmt.Sprintf("SELECT COUNT(*) FROM %s", tableName),
	).Scan(&count)
	return count
}

func TestInsertDenormalizedDiffRows(t *testing.T) {
	tableName := "denormalized_view_recent_project"
	defer clearTable(tableName)

	assert.Equal(t, 0, getRowCountAtTable(tableName), "Rows should start empty")

	d, err := repositories.NewScopedDenormalizerRepository(fakeTarget, fakeMappedTarget)
	assert.Equal(t, nil, err, "Error should not be nil")

	fixtures := fakeFixtures()
	err = d.DenormalizeToRecentView(fixtures)
	assert.Equal(t, nil, err, "Error should be nil")
	assert.Equal(t, len(fixtures), getRowCountAtTable(tableName), "Rows should be inserted")
}

func TestDenormalizeToChangesOverTime(t *testing.T) {
	defer clearTable("denormalized_view_changes_over_time")

	d, _ := repositories.NewScopedDenormalizerRepository(fakeTarget, fakeMappedTarget)
	fixtures := fakeFixtures()
	fixtures[0].DBInsertTimestamp = 1519666754
	err := d.DenormalizeToChangesOverTime(fixtures)
	assert.Equal(t, nil, err, "Error should be nil")
}

func TestDenormalizeToRecentCommits(t *testing.T) {
	tableName := "denormalized_view_recent_commit"
	defer clearTable(tableName)
	d, _ := repositories.NewScopedDenormalizerRepository(fakeTarget, fakeMappedTarget)
	fixture := e.AnalyzedCommitRow{
		CommitRow: e.CommitRow{
			Date:              "2018/02/20",
			Commit:            "61d5e61b6b6dfbf52d0d433759da964db31cc106",
			DownstreamProject: "platform/vendor/unbundled_google/packages/Ears",
			Author:            "slobdell@google.com",
			// Actual commit subject!
			Subject: "Import translations. DO NOT MERGE",
		},
	}
	fixtures := []e.AnalyzedCommitRow{
		fixture,
	}
	err := d.DenormalizeToRecentCommits(
		fixtures,
		map[string]e.RepoTimestamp{
			"61d5e61b6b6dfbf52d0d433759da964db31cc106": arbitraryTimestamp,
		},
	)
	assert.Equal(t, nil, err, "Error should be nil")
	assert.Equal(t, len(fixtures), getRowCountAtTable(tableName), "Rows should be inserted")
}

func TestDenormalizeToTopCommitter(t *testing.T) {
	tableName := "denormalized_view_top_committer"
	defer clearTable(tableName)
	defer clearTable("denormalized_view_recent_commit")
	defer clearTable("project_commit")

	fakeCommitRows := []e.AnalyzedCommitRow{
		e.AnalyzedCommitRow{
			CommitRow: e.CommitRow{
				Date:              "2018/03/20",
				Commit:            "540eecd728a407e4b31a38f4ea9416dea7d05c0c",
				DownstreamProject: "platform/tools/external/gradle",
				Author:            "jeffrey.lebowski@google.com",
				Subject:           "Hand off the briefcase",
			},
			Type: c.Empty,
		},
		e.AnalyzedCommitRow{
			CommitRow: e.CommitRow{
				Date:              "2018/03/19",
				Commit:            "ea999655a8af4b7d6a8033d1c864ca87617d0ede",
				DownstreamProject: "platform/tools/external/gradle",
				Author:            "brandt@google.com",
				Subject:           "We Just Don't Know",
			},
			Type: c.Empty,
		},
		e.AnalyzedCommitRow{
			CommitRow: e.CommitRow{
				Date:              "2018/03/19",
				Commit:            "4cc9725c953f57f8abe63b729e26125feac1be4e",
				DownstreamProject: "platform/tools/external/gradle",
				Author:            "jeffrey.lebowski@google.com",
				Subject:           "Take any rug in the house",
			},
			Type: c.Empty,
		},
	}
	commitRepo, err := repositories.NewCommitRepository(fakeMappedTarget)
	err = commitRepo.InsertCommitRows(fakeCommitRows)
	assert.Equal(t, nil, err, "Error should be nil")
	assert.Equal(t, 3, getRowCountAtTable("project_commit"), "Rows should be inserted")

	scopedD, _ := repositories.NewScopedDenormalizerRepository(fakeTarget, fakeMappedTarget)

	err = scopedD.DenormalizeToRecentCommits(
		fakeCommitRows,
		map[string]e.RepoTimestamp{
			"540eecd728a407e4b31a38f4ea9416dea7d05c0c": arbitraryTimestamp,
			"ea999655a8af4b7d6a8033d1c864ca87617d0ede": arbitraryTimestamp,
			"4cc9725c953f57f8abe63b729e26125feac1be4e": arbitraryTimestamp,
		},
	)
	assert.Equal(t, nil, err, "Error should be nil")
	assert.Equal(t, 3, getRowCountAtTable("denormalized_view_recent_commit"), "Rows should be inserted")

	d, _ := repositories.NewGlobalDenormalizerRepository()
	err = d.DenormalizeToTopCommitter()
	assert.Equal(t, nil, err, "Error should be nil")
	assert.Equal(t, 2, getRowCountAtTable(tableName), "Rows should be inserted")
}

func TestDenormalizeToTopTechArea(t *testing.T) {
	tableName := "denormalized_view_top_tech_area"
	defer clearTable(tableName)
	defer clearTable("denormalized_view_recent_commit")
	defer clearTable("project_commit")
	fakeCommitRows := []e.AnalyzedCommitRow{
		e.AnalyzedCommitRow{
			CommitRow: e.CommitRow{
				Date:              "2018/03/20",
				Commit:            "540eecd728a407e4b31a38f4ea9416dea7d05c0c",
				DownstreamProject: "platform/tools/external/gradle",
				Author:            "jeffrey.lebowski@google.com",
				Subject:           "Hand off the briefcase",
			},
			Type: c.Empty,
		},
		e.AnalyzedCommitRow{
			CommitRow: e.CommitRow{
				Date:              "2018/03/19",
				Commit:            "ea999655a8af4b7d6a8033d1c864ca87617d0ede",
				DownstreamProject: "platform/tools/external/gradle",
				Author:            "brandt@google.com",
				Subject:           "We Just Don't Know",
			},
			Type: c.Empty,
		},
		e.AnalyzedCommitRow{
			CommitRow: e.CommitRow{
				Date:              "2018/03/19",
				Commit:            "4cc9725c953f57f8abe63b729e26125feac1be4e",
				DownstreamProject: "platform/tools/external/gradle",
				Author:            "jeffrey.lebowski@google.com",
				Subject:           "Take any rug in the house",
			},
			Type: c.Empty,
		},
	}
	commitRepo, err := repositories.NewCommitRepository(fakeMappedTarget)
	err = commitRepo.InsertCommitRows(fakeCommitRows)
	assert.Equal(t, nil, err, "Error should be nil")
	assert.Equal(t, 3, getRowCountAtTable("project_commit"), "Rows should be inserted")

	scopedD, _ := repositories.NewScopedDenormalizerRepository(fakeTarget, fakeMappedTarget)

	err = scopedD.DenormalizeToRecentCommits(
		fakeCommitRows,
		map[string]e.RepoTimestamp{
			"540eecd728a407e4b31a38f4ea9416dea7d05c0c": arbitraryTimestamp,
			"ea999655a8af4b7d6a8033d1c864ca87617d0ede": arbitraryTimestamp,
			"4cc9725c953f57f8abe63b729e26125feac1be4e": arbitraryTimestamp,
		},
	)
	assert.Equal(t, nil, err, "Error should be nil")
	assert.Equal(t, 3, getRowCountAtTable("denormalized_view_recent_commit"), "Rows should be inserted")

	d, _ := repositories.NewGlobalDenormalizerRepository()
	err = d.DenormalizeToTopTechArea()
	assert.Equal(t, nil, err, "Error should be nil")
	assert.Equal(t, 2, getRowCountAtTable(tableName), "Rows should be inserted")
}
