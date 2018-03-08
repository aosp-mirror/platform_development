package filesystem

import (
	"fmt"
	"os/exec"
	"strings"

	"github.com/pkg/errors"
)

type mapperFn func([]string) (interface{}, error)

func CSVFileToEntities(csvFile string, mapper mapperFn) ([]interface{}, error) {
	var errMapping error
	var entityRows []interface{}

	errReading := GenerateCSVLines(
		csvFile,
		func(columns []string) {
			if errMapping != nil {
				return
			}
			var entity interface{}
			entity, errMapping = mapper(columns)
			if errMapping == nil {
				entityRows = append(entityRows, entity)
			}
		},
	)
	if errReading != nil {
		return nil, errors.Wrap(errReading, fmt.Sprintf("Error reading %s file from filesystem", csvFile))
	}
	if errMapping != nil {
		return nil, errors.Wrap(errMapping, "Error mapping CSV lines to entities")
	}
	return entityRows, nil
}

func FindFnamesInDir(directory string, filenames ...string) []string {
	var outputFilenames []string
	for _, filename := range filenames {
		findProjectsCmd := fmt.Sprintf("find %s | grep %s", directory, filename)
		out, err := exec.Command("bash", "-c", findProjectsCmd).Output()
		if err != nil {
			return nil
		}
		outputFilenames = append(
			outputFilenames,
			filterEmptyStrings(strings.Split(string(out), "\n"))...,
		)
	}
	return outputFilenames
}

func filterEmptyStrings(strings []string) []string {
	filtered := make([]string, len(strings)-countEmpty(strings))
	copyToIndex := 0
	for _, str := range strings {
		if str != "" {
			filtered[copyToIndex] = str
			copyToIndex++
		}
	}
	return filtered
}

func countEmpty(strings []string) int {
	numEmpty := 0
	for _, str := range strings {
		if str == "" {
			numEmpty++
		}
	}
	return numEmpty
}
