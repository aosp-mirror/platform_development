package repositories_test

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"

	repoSQL "repodiff/persistence/sql"
	"repodiff/repositories"
)

func init() {
	defer clearTable("denormalized_view_recent_project")
	defer clearTable("denormalized_view_changes_over_time")
}

func clearTable(tableName string) {
	db, _ := repoSQL.GetDBConnectionPool()
	db.Exec(fmt.Sprintf("TRUNCATE TABLE %s", tableName))
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

	d, err := repositories.NewDenormalizerRepository()
	assert.Equal(t, nil, err, "Error should not be nil")

	fixtures := fakeFixtures()
	d.DenormalizeToRecentView(fixtures)
	assert.Equal(t, len(fixtures), getRowCountAtTable(tableName), "Rows should be inserted")
}

func TestDenormalizeToChangesOverTime(t *testing.T) {
	defer clearTable("denormalized_view_changes_over_time")

	d, _ := repositories.NewDenormalizerRepository()
	fixtures := fakeFixtures()
	fixtures[0].DBInsertTimestamp = 1519666754
	err := d.DenormalizeToChangesOverTime(fixtures)
	assert.Equal(t, nil, err, "Error should be nil")
}
