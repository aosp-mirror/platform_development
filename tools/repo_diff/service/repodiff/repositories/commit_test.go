package repositories_test

import (
	"testing"

	"github.com/stretchr/testify/assert"

	e "repodiff/entities"
	repoSQL "repodiff/persistence/sql"
	"repodiff/repositories"
)

func init() {
	clearCommitTable()
}

func getCommitRowCount() int {
	db, _ := repoSQL.GetDBConnectionPool()
	var count int
	db.QueryRow("SELECT COUNT(*) FROM project_commit").Scan(&count)
	return count
}

func clearCommitTable() {
	db, _ := repoSQL.GetDBConnectionPool()
	db.Exec("TRUNCATE TABLE project_commit")
}

func TestInsertCommitRows(t *testing.T) {
	defer clearCommitTable()

	assert.Equal(t, 0, getCommitRowCount(), "Rows should start empty")

	c, err := repositories.NewCommitRepository()
	assert.Equal(t, nil, err, "Error should not be nil")

	fixture := e.CommitRow{
		Date:              "2018/02/20",
		Commit:            "61d5e61b6b6dfbf52d0d433759da964db31cc106",
		DownstreamProject: "platform/vendor/unbundled_google/packages/Ears",
		Author:            "slobdell@google.com",
		// Actual commit subject!
		Subject: "Import translations. DO NOT MERGE",
	}
	fixtures := []e.CommitRow{
		fixture,
	}
	err = c.InsertCommitRows(fixtures)
	assert.Equal(t, nil, err, "Error should be nil")
	assert.Equal(t, len(fixtures), getCommitRowCount(), "Rows should be inserted")
}
