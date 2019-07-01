package repositories_test

import (
	"testing"

	"github.com/stretchr/testify/assert"

	c "repodiff/constants"
	e "repodiff/entities"
	repoSQL "repodiff/persistence/sql"
	"repodiff/repositories"
	"repodiff/utils"
)

func init() {
	clearProjectTable()
}

var testDiffTarget = e.MappedDiffTarget{
	UpstreamTarget:   int16(1),
	DownstreamTarget: int16(1),
}

func getProjectRowCount() int {
	db, _ := repoSQL.GetDBConnectionPool()
	var count int
	db.QueryRow("SELECT COUNT(*) FROM project_differential").Scan(&count)
	return count
}

func clearProjectTable() {
	db, _ := repoSQL.GetDBConnectionPool()
	db.Exec("TRUNCATE TABLE project_differential")
}

func TestInsertDiffRows(t *testing.T) {
	defer clearProjectTable()

	assert.Equal(t, 0, getProjectRowCount(), "Rows should start empty")

	p, err := repositories.NewProjectRepository(testDiffTarget)
	assert.Equal(t, nil, err, "Error should not be nil")

	fixtures := fakeFixtures()
	p.InsertDiffRows(fixtures)
	assert.Equal(t, len(fixtures), getProjectRowCount(), "Rows should be inserted")
}

func TestGetMostRecentOuterKey(t *testing.T) {
	defer clearProjectTable()
	p, _ := repositories.NewProjectRepository(testDiffTarget)
	p.InsertDiffRows(fakeFixtures())

	var oldTimestamp int64 = 1519333790
	timestamp, uid, _ := p.GetMostRecentOuterKey()

	assert.True(t, timestamp > oldTimestamp, "Insert timestamp should be greater than old")
	assert.Equal(t, 36, len(uid.String()), "Valid UUID should be generated")
}

func TestGetMostRecentOuterKeyEmpty(t *testing.T) {
	assert.Equal(t, 0, getProjectRowCount(), "Database shoudl start empty")

	p, _ := repositories.NewProjectRepository(testDiffTarget)
	_, _, err := p.GetMostRecentOuterKey()
	assert.NotEqual(t, nil, err, "Error should be returned when database is empty")
}

func TestGetMostRecentDifferentials(t *testing.T) {
	defer clearProjectTable()
	p, _ := repositories.NewProjectRepository(testDiffTarget)
	dateNow := utils.TimestampToDate(utils.TimestampSeconds())
	fixtures := fakeFixtures()

	fixtures[0].Date = dateNow
	p.InsertDiffRows(fixtures)
	diffRows, err := p.GetMostRecentDifferentials()
	assert.Equal(t, nil, err, "Error should not be nil")
	assert.Equal(t, 1, len(diffRows), "1 result should exist")

	expected := e.AnalyzedDiffRow{
		DiffRow: e.DiffRow{
			Date:                 dateNow,
			DownstreamProject:    "platform/vendor/unbundled_google/packages/Ears",
			UpstreamProject:      "platform/vendor/unbundled_google/packages/Ears",
			DiffStatus:           3,
			FilesChanged:         34,
			LineInsertions:       8,
			LineDeletions:        25,
			LineChanges:          32,
			CommitsNotUpstreamed: 0,
		},
		Type: c.Empty,
	}
	d := diffRows[0]

	// not concerned about direct comparison
	expected.DBInsertTimestamp = d.DBInsertTimestamp
	assert.Equal(t, expected, d, "Results should be equal")
}

func TestGetMostRecentDifferentialsEmpty(t *testing.T) {
	p, _ := repositories.NewProjectRepository(testDiffTarget)
	rows, err := p.GetMostRecentDifferentials()
	assert.Equal(t, nil, err, "Error should be nil")
	assert.Equal(t, 0, len(rows))
}
