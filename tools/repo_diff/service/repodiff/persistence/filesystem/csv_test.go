package filesystem

import (
	"os"
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

func TestWriteCSVToFile(t *testing.T) {
	outputPath := "testdata/generate.csv"
	defer os.Remove(outputPath)

	header := []string{
		"col1",
		"col2",
	}
	rowsOfCols := [][]string{
		[]string{
			"line1_val1",
			"line1_val2",
		},
		[]string{
			"line2_val1",
			"line2_val2",
		},
	}
	err := WriteCSVToFile(header, rowsOfCols, outputPath)
	assert.Equal(t, err, nil, "Error should not be generated")

	var reconstituted [][]string
	err = GenerateCSVLines(
		outputPath,
		func(columns []string) {
			assert.Equal(t, 2, len(columns), "Initial CSV ColumnCount")
			reconstituted = append(reconstituted, columns)
		},
	)
	assert.Equal(t, nil, err, "No error should exist reading created CSV")
	assert.Equal(t, rowsOfCols, reconstituted, "Reconstituted should equal original")
}
