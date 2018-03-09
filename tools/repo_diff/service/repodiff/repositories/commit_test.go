package repositories_test

import (
	"testing"

	"github.com/stretchr/testify/assert"

	repoSQL "repodiff/persistence/sql"
	"repodiff/repositories"
	"repodiff/utils"
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

	c, err := repositories.NewCommitRepository(fakeMappedTarget)
	assert.Equal(t, nil, err, "Error should not be nil")

	fixtures := fakeCommitFixtures()
	err = c.InsertCommitRows(fixtures)
	assert.Equal(t, nil, err, "Error should be nil")
	assert.Equal(t, len(fixtures), getCommitRowCount(), "Rows should be inserted")
}

func TestCommitGetMostRecentOuterKey(t *testing.T) {
	defer clearCommitTable()
	c, _ := repositories.NewCommitRepository(fakeMappedTarget)
	fixtures := fakeCommitFixtures()
	err := c.InsertCommitRows(fixtures)
	assert.Equal(t, nil, err, "Eroror should be nil")

	var oldTimestamp int64 = 1519333790
	timestamp, uid, _ := c.GetMostRecentOuterKey()
	assert.True(t, timestamp > oldTimestamp, "Insert timestamp should be greater than old")
	assert.Equal(t, 36, len(uid.String()), "Valid UUID should be generated")
}

func TestGetMostRecentCommits(t *testing.T) {
	defer clearCommitTable()

	c, _ := repositories.NewCommitRepository(fakeMappedTarget)
	dateNow := utils.TimestampToDate(utils.TimestampSeconds())

	fixtures := fakeCommitFixtures()

	fixtures[0].Date = dateNow
	c.InsertCommitRows(fixtures)
	commitRows, err := c.GetMostRecentCommits()
	assert.Equal(t, nil, err, "Error should not be nil")
	assert.Equal(t, 1, len(commitRows), "1 result should exist")
}

func TestGetMostRecentCommitsEmpty(t *testing.T) {
	c, _ := repositories.NewCommitRepository(testDiffTarget)
	rows, err := c.GetMostRecentCommits()
	assert.Equal(t, nil, err, "Error should be nil")
	assert.Equal(t, 0, len(rows))
}
