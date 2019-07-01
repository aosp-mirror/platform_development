package repositories_test

import (
	"testing"

	"github.com/stretchr/testify/assert"

	ent "repodiff/entities"
	repoSQL "repodiff/persistence/sql"
	"repodiff/repositories"
	"repodiff/utils"
)

func init() {
	clearTableBeforeAfterTest("project_commit")()
}

func getCommitRowCount() int {
	db, _ := repoSQL.GetDBConnectionPool()
	var count int
	db.QueryRow("SELECT COUNT(*) FROM project_commit").Scan(&count)
	return count
}

func TestInsertCommitRows(t *testing.T) {
	defer clearTableBeforeAfterTest("project_commit")()

	assert.Equal(t, 0, getCommitRowCount(), "Rows should start empty")

	c, err := repositories.NewCommitRepository(fakeMappedTarget)
	assert.Equal(t, nil, err, "Error should not be nil")

	fixtures := fakeCommitFixtures()
	err = c.InsertCommitRows(fixtures)
	assert.Equal(t, nil, err, "Error should be nil")
	assert.Equal(t, len(fixtures), getCommitRowCount(), "Rows should be inserted")
}

func TestCommitGetMostRecentOuterKey(t *testing.T) {
	defer clearTableBeforeAfterTest("project_commit")()
	c, _ := repositories.NewCommitRepository(fakeMappedTarget)
	fixtures := fakeCommitFixtures()
	err := c.InsertCommitRows(fixtures)
	assert.Equal(t, nil, err, "Eroror should be nil")

	var oldTimestamp ent.RepoTimestamp = 1519333790
	timestamp, uid, _ := c.GetMostRecentOuterKey()
	assert.True(t, ent.RepoTimestamp(timestamp) > oldTimestamp, "Insert timestamp should be greater than old")
	assert.Equal(t, 36, len(uid.String()), "Valid UUID should be generated")
}

func TestGetMostRecentCommits(t *testing.T) {
	defer clearTableBeforeAfterTest("project_commit")()

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

func TestGetFirstSeenTimestamp(t *testing.T) {
	defer clearTableBeforeAfterTest("project_commit")()
	c, _ := repositories.NewCommitRepository(fakeMappedTarget)
	fixtures := fakeCommitFixtures()
	oldFakeTimestamp := ent.RepoTimestamp(1)
	c.WithTimestampGenerator(
		func() ent.RepoTimestamp { return oldFakeTimestamp },
	).InsertCommitRows(fixtures)

	newFakeTimestamp := ent.RepoTimestamp(2)
	c.WithTimestampGenerator(
		func() ent.RepoTimestamp { return newFakeTimestamp },
	).InsertCommitRows(fixtures)

	commitHashes := []string{
		"61d5e61b6b6dfbf52d0d433759da964db31cc106",
	}
	nullTimestamp := ent.RepoTimestamp(0)
	commitToTimestamp, err := c.GetFirstSeenTimestamp(commitHashes, nullTimestamp)
	assert.Equal(t, nil, err, "Error should be nil")
	assert.Equal(t, len(commitHashes), len(commitToTimestamp), "Length of returned values")
	assert.Equal(t, oldFakeTimestamp, commitToTimestamp["61d5e61b6b6dfbf52d0d433759da964db31cc106"], "Expected returned timestamp")
}

func TestGetFirstSeenTimestampEmpty(t *testing.T) {
	c, _ := repositories.NewCommitRepository(fakeMappedTarget)
	nullTimestamp := ent.RepoTimestamp(0)
	commitToTimestamp, err := c.GetFirstSeenTimestamp([]string{}, nullTimestamp)
	assert.Equal(t, nil, err, "Error should be nil")
	assert.Equal(t, 0, len(commitToTimestamp), "Length of returned values")
}

func TestGetFirstSeenTimestampMutateReturned(t *testing.T) {
	c, _ := repositories.NewCommitRepository(fakeMappedTarget)
	nullTimestamp := ent.RepoTimestamp(0)
	commitToTimestamp, _ := c.GetFirstSeenTimestamp([]string{}, nullTimestamp)
	commitToTimestamp["some_key"] = ent.RepoTimestamp(0)
}

func TestGetFirstSeenTimestampNonExistent(t *testing.T) {
	c, _ := repositories.NewCommitRepository(fakeMappedTarget)
	nonExistentHash := "ae8e745ba09f61ddfa46ed6bba54c4bd07b2e93b"
	nullTimestamp := ent.RepoTimestamp(123)
	nonExistentHashes := []string{nonExistentHash}
	commitToTimestamp, err := c.GetFirstSeenTimestamp(nonExistentHashes, nullTimestamp)
	assert.Equal(t, nil, err, "Error should not be generated")
	assert.Equal(t, len(nonExistentHashes), len(commitToTimestamp), "Fetched results should match the length of the input")
	assert.Equal(t, nullTimestamp, commitToTimestamp[nonExistentHash], "Populated value should equal the input null timestamp")
}
