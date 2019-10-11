package repositories

import (
	"testing"

	lru "github.com/hashicorp/golang-lru"
	"github.com/stretchr/testify/assert"

	e "repodiff/entities"
	repoSQL "repodiff/persistence/sql"
)

func init() {
	clearIDTable()
}

func tearDown() {
	clearIDTable()
	cacheSingleton, _ = lru.New(cacheSize)
}

func clearIDTable() {
	db, _ := repoSQL.GetDBConnectionPool()
	db.Exec("TRUNCATE TABLE id_to_url_branch")
}

func TestProtocolStrippedURL(t *testing.T) {
	urlWithProtocol := "https://keystone-qcom.googlesource.com/platform/manifest"
	expected := "//keystone-qcom.googlesource.com/platform/manifest"
	assert.Equal(t, expected, protocolStrippedURL(urlWithProtocol), "Protocol should be removed")
}

func TestGetOrCreateURLBranchID(t *testing.T) {
	defer tearDown()
	url := "https://keystone-qcom.googlesource.com/platform/manifest"
	branch := "p-fs-release"

	sourceRepo, _ := NewSourceRepository()
	id, err := sourceRepo.getOrCreateURLBranchID(url, branch)
	assert.Equal(t, nil, err, "Error should be nil")
	assert.True(t, id > 0, "ID should be non-zero")

	idSecondFetch, err := sourceRepo.getOrCreateURLBranchID(url, branch)
	assert.Equal(t, id, idSecondFetch, "Retrieved ID values should be identical")
}

func TestGetOrCreateURLBranchIDCache(t *testing.T) {
	// this test relies on the assumption that database rows will be immutable in production
	defer tearDown()
	url := "https://keystone-qcom.googlesource.com/platform/manifest"
	branch := "p-fs-release"

	sourceRepo, _ := NewSourceRepository()
	id, _ := sourceRepo.getOrCreateURLBranchID(url, branch)
	assert.Equal(t, int16(1), id, "ID should be 1 since it's first row")

	clearIDTable()
	nextURL := "https://totally-new-url-to-replace-first-row/platform/manifest"
	nextBranch := "master"
	id, _ = sourceRepo.getOrCreateURLBranchID(nextURL, nextBranch)
	assert.Equal(t, int16(1), id, "ID should be 1 since it's first row")

	id, _ = sourceRepo.getOrCreateURLBranchID(url, branch)
	assert.Equal(t, int16(1), id, "ID should be 1 since it hit the cache")
}

func TestGetURLBranchByID(t *testing.T) {
	defer tearDown()
	url := "https://keystone-qcom.googlesource.com/platform/manifest"
	branch := "p-fs-release"

	sourceRepo, _ := NewSourceRepository()
	id, _ := sourceRepo.getOrCreateURLBranchID(url, branch)
	assert.Equal(t, int16(1), id, "ID should be 1 since it's first row")

	fetchedURL, fetchedBranch, err := sourceRepo.GetURLBranchByID(id)
	assert.Equal(t, nil, err, "Error should be nil")
	assert.Equal(t, "//keystone-qcom.googlesource.com/platform/manifest", fetchedURL, "Fetched URL should be equal")
	assert.Equal(t, branch, fetchedBranch, "Fetched branch should be equal")
}

func TestGetURLBranchByIDCache(t *testing.T) {
	// this test relies on the assumption that database rows will be immutable in production
	defer tearDown()
	url := "https://keystone-qcom.googlesource.com/platform/manifest"
	branch := "p-fs-release"

	sourceRepo, _ := NewSourceRepository()
	id, _ := sourceRepo.getOrCreateURLBranchID(url, branch)
	assert.Equal(t, int16(1), id, "ID should be 1 since it's first row")
	fetchedURL, _, _ := sourceRepo.GetURLBranchByID(id)
	assert.Equal(t, "//keystone-qcom.googlesource.com/platform/manifest", fetchedURL, "Fetched URL should be equal")
	clearIDTable()

	nextURL := "https://totally-new-url-to-replace-first-row/platform/manifest"
	nextBranch := "master"
	id, _ = sourceRepo.getOrCreateURLBranchID(nextURL, nextBranch)
	assert.Equal(t, int16(1), id, "ID should be 1 since it's first row")

	fetchedURL, _, _ = sourceRepo.GetURLBranchByID(int16(1))
	assert.Equal(t, "//keystone-qcom.googlesource.com/platform/manifest", fetchedURL, "Fetched URL should equal first value because of hitting cache")
}

func TestGetDiffTargetToMapped(t *testing.T) {
	defer tearDown()

	target := e.DiffTarget{
		Upstream: e.Project{
			URL:    "https://keystone-qcom.googlesource.com/platform/manifest",
			Branch: "p-fs-release",
		},
		Downstream: e.Project{
			URL:    "https://keystone-qcom.googlesource.com/platform/manifest",
			Branch: "p-keystone-qcom",
		},
	}
	sourceRepo, _ := NewSourceRepository()
	mappedTarget, _ := sourceRepo.DiffTargetToMapped(target)

	assert.Equal(t, int16(1), mappedTarget.UpstreamTarget, "Expected value for upstream")
	assert.Equal(t, int16(2), mappedTarget.DownstreamTarget, "Expected value for downstream")
}
