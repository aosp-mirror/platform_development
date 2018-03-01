package filesystem

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestFindFnamesInDirNonZero(t *testing.T) {
	foundFiles := FindFnamesInDir("testdata", "project.csv")
	assert.Equal(t, 1, len(foundFiles), "Number of files found in directory")
}

func TestFindFnamesInDirMultipleArgs(t *testing.T) {
	foundFiles := FindFnamesInDir("testdata", "project.csv", "commit.csv")
	assert.Equal(t, 2, len(foundFiles), "Number of files found in directory")
}

func TestFindFnamesInDirZero(t *testing.T) {
	foundFiles := FindFnamesInDir("testdata", "totally_should_not_exist.xyz")
	assert.Equal(t, 0, len(foundFiles), "Number of files found in directory")
}

func TestCSVFileToEntities(t *testing.T) {
	entities, err := CSVFileToEntities(
		"testdata/project.csv",
		func(cols []string) (interface{}, error) {
			return cols, nil
		},
	)
	assert.Equal(t, nil, err, "Error should be nil")
	assert.Equal(t, 670, len(entities), "Entity length should be equal")
}
