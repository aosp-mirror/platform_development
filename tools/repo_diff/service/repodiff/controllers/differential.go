package controllers

import (
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"time"

	"github.com/pkg/errors"

	e "repodiff/entities"
	"repodiff/interactors"
	"repodiff/mappers"
	"repodiff/persistence/filesystem"
	"repodiff/repositories"
)

var expectedOutputFilenames = []string{
	"project.csv",
	"commit.csv",
}

// Executes all of the differentials specified in the application config.
// While each target is executed synchronously, the differential script is already multi-threaded
// across all of the local machine's cores, so there is no benefit to parallelizing multiple differential
// targets
func ExecuteDifferentials(config e.ApplicationConfig) error {
	err := createWorkingPath(config.OutputDirectory)
	if err != nil {
		return errors.Wrap(err, "Could not create working path")
	}

	for _, target := range config.DiffTargets {
		commitCSV, projectCSV, err := runPyScript(config, target)
		if err != nil {
			return errors.Wrap(err, "Error running python differential script")
		}
		err = TransferScriptOutputToDownstream(projectCSV, commitCSV)
		if err != nil {
			return errors.Wrap(err, "Error transferring script output to downstream")
		}
	}
	return nil
}

func createWorkingPath(folderPath string) error {
	return os.MkdirAll(folderPath, os.ModePerm)
}

func printFunctionDuration(fnLabel string, start time.Time) {
	fmt.Printf("Finished '%s' in %s\n", fnLabel, time.Now().Sub(start))
}

func setupCommand(pyScript string, config e.ApplicationConfig, target e.DiffTarget) *exec.Cmd {
	cmd := exec.Command(
		"python",
		pyScript,
		"--manifest-url",
		target.Downstream.URL,
		"--manifest-branch",
		target.Downstream.Branch,
		"--upstream-manifest-url",
		target.Upstream.URL,
		"--upstream-manifest-branch",
		target.Upstream.Branch,
	)
	cmd.Dir = config.OutputDirectory
	return cmd
}

func runPyScript(config e.ApplicationConfig, target e.DiffTarget) (projectCSV string, commitCSV string, err error) {
	pyScript := filepath.Join(
		config.AndroidProjectDir,
		config.DiffScript,
	)
	outFilesBefore := filesystem.FindFnamesInDir(config.OutputDirectory, expectedOutputFilenames...)
	err = diffTarget(pyScript, config, target)
	if err != nil {
		return "", "", err
	}
	outFilesAfter := filesystem.FindFnamesInDir(config.OutputDirectory, expectedOutputFilenames...)
	newFiles := interactors.Difference(outFilesBefore, outFilesAfter)
	if len(newFiles) != 2 {
		return "", "", errors.New("Expected 1 new output file. A race condition exists")
	}
	return newFiles[0], newFiles[1], nil
}

func diffTarget(pyScript string, config e.ApplicationConfig, target e.DiffTarget) error {
	defer printFunctionDuration("Run Differential", time.Now())
	cmd := setupCommand(pyScript, config, target)

	displayStr := strings.Join(cmd.Args, " ")
	fmt.Printf("Executing command:\n\n%s\n\n", displayStr)

	return errors.Wrap(
		cmd.Run(),
		fmt.Sprintf(
			"Failed to execute (%s). Ensure glogin has been run or update application config to provide correct parameters",
			displayStr,
		),
	)
}

// SBL need to add test coverage here
func TransferScriptOutputToDownstream(projectCSVFile, commitCSVFile string) error {
	diffRows, commitRows, err := readCSVFiles(projectCSVFile, commitCSVFile)
	if err != nil {
		return err
	}
	return persistEntities(diffRows, commitRows)
}

func readCSVFiles(projectCSVFile, commitCSVFile string) ([]e.DiffRow, []e.CommitRow, error) {
	diffRows, err := csvFileToDiffRows(projectCSVFile)
	if err != nil {
		return nil, nil, errors.Wrap(err, "Error converting CSV file to entities")
	}
	commitRows, err := csvFileToCommitRows(commitCSVFile)
	if err != nil {
		return nil, nil, errors.Wrap(err, "Error converting CSV file to entities")
	}
	return diffRows, commitRows, nil
}

func persistEntities(diffRows []e.DiffRow, commitRows []e.CommitRow) error {
	err := persistDiffRowsDownstream(diffRows)
	if err != nil {
		return errors.Wrap(err, "Error persisting diff rows")
	}

	err = persistCommitRowsDownstream(commitRows)
	if err != nil {
		return errors.Wrap(err, "Error persist commit rows")
	}
	return nil
}

func csvFileToDiffRows(csvFile string) ([]e.DiffRow, error) {
	entities, err := filesystem.CSVFileToEntities(
		csvFile,
		func(cols []string) (interface{}, error) {
			return mappers.CSVLineToDiffRow(cols)
		},
	)
	if err != nil {
		return nil, err
	}
	return toDiffRows(entities)
}

func toDiffRows(entities []interface{}) ([]e.DiffRow, error) {
	diffRows := make([]e.DiffRow, len(entities))
	for i, entity := range entities {
		diffRow, ok := entity.(*e.DiffRow)
		if !ok {
			return nil, errors.New("Error casting to DiffRow")
		}
		diffRows[i] = *diffRow
	}
	return diffRows, nil
}

func csvFileToCommitRows(csvFile string) ([]e.CommitRow, error) {
	entities, err := filesystem.CSVFileToEntities(
		csvFile,
		func(cols []string) (interface{}, error) {
			return mappers.CSVLineToCommitRow(cols)
		},
	)
	if err != nil {
		return nil, err
	}
	return toCommitRows(entities)
}

func toCommitRows(entities []interface{}) ([]e.CommitRow, error) {
	commitRows := make([]e.CommitRow, len(entities))
	for i, entity := range entities {
		commitRow, ok := entity.(*e.CommitRow)
		if !ok {
			return nil, errors.New("Error casting to CommitRow")
		}
		commitRows[i] = *commitRow
	}
	return commitRows, nil
}

func persistDiffRowsDownstream(diffRows []e.DiffRow) error {
	p, err := repositories.NewProjectRepository()
	if err != nil {
		return errors.Wrap(err, "Error instantiating a new project repository")
	}
	err = p.InsertDiffRows(diffRows)
	if err != nil {
		return errors.Wrap(err, "Error inserting rows from controller")
	}
	return nil
}

func persistCommitRowsDownstream(commitRows []e.CommitRow) error {
	c, err := repositories.NewCommitRepository()
	if err != nil {
		return errors.Wrap(err, "Error instantiating a new commit repository")
	}
	err = c.InsertCommitRows(commitRows)
	if err != nil {
		return errors.Wrap(err, "Error inserting rows from controller")
	}
	return nil
}
