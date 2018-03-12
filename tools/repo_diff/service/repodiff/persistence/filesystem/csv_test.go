package filesystem

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestGenerateCSVLines(t *testing.T) {
	lineCount := 0
	err := GenerateCSVLines(
		"testdata/project.csv",
		func(columns []string) {
			lineCount++
			assert.Equal(t, 9, len(columns), "Fixture CSV Column Count")
		},
	)
	assert.Equal(t, 670, lineCount, "Fixture CSV line count")
	assert.Equal(t, nil, err, "Read CSV Error output")
}

func TestNonExistentFile(t *testing.T) {

	err := GenerateCSVLines(
		"this_file_totally_does_not_exist.csv",
		func(columns []string) {},
	)
	assert.NotEqual(t, err, nil, "CSV Error should be generated")
}
