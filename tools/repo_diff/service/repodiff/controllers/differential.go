package controllers

import (
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"time"

	"github.com/pkg/errors"

	ent "repodiff/entities"
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
func ExecuteDifferentials(config ent.ApplicationConfig) error {
	err := createWorkingPath(config.OutputDirectory)
	if err != nil {
		return errors.Wrap(err, "Could not create working path")
	}

	commonManifest, err := defineCommonManifest(config)
	if err != nil {
		return err
	}

	for _, target := range config.DiffTargets {
		fmt.Printf("Processing differential from %s to %s\n", target.Upstream.Branch, target.Downstream.Branch)
		err = clearOutputDirectory(config)
		commitCSV, projectCSV, err := runPyScript(config, target)
		if err != nil {
			return errors.Wrap(err, "Error running python differential script")
		}
		err = TransferScriptOutputToDownstream(config, target, projectCSV, commitCSV, commonManifest)
		if err != nil {
			return errors.Wrap(err, "Error transferring script output to downstream")
		}
	}
	return nil
}

func defineCommonManifest(config ent.ApplicationConfig) (*ent.ManifestFile, error) {
	workingDirectory := filepath.Join(config.OutputDirectory, "common_upstream")
	if err := createWorkingPath(workingDirectory); err != nil {
		return nil, err
	}
	cmd := exec.Command(
		"bash",
		"-c",
		fmt.Sprintf(
			"repo init -u %s -b %s",
			config.CommonUpstream.URL,
			config.CommonUpstream.Branch,
		),
	)
	cmd.Dir = workingDirectory
	if _, err := cmd.Output(); err != nil {
		return nil, err
	}

	var manifest ent.ManifestFile
	err := filesystem.ReadXMLAsEntity(
		// the output of repo init will generate a manifest file at this location
		filepath.Join(workingDirectory, ".repo/manifest.xml"),
		&manifest,
	)
	return &manifest, err
}

func createWorkingPath(folderPath string) error {
	return os.MkdirAll(folderPath, os.ModePerm)
}

func printFunctionDuration(fnLabel string, start time.Time) {
	fmt.Printf("Finished '%s' in %s\n", fnLabel, time.Now().Sub(start))
}

func clearOutputDirectory(config ent.ApplicationConfig) error {
	return exec.Command(
		"/bin/sh",
		"-c",
		fmt.Sprintf("rm -rf %s/*", config.OutputDirectory),
	).Run()
}

func setupCommand(pyScript string, config ent.ApplicationConfig, target ent.DiffTarget) *exec.Cmd {
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

func runPyScript(config ent.ApplicationConfig, target ent.DiffTarget) (projectCSV string, commitCSV string, err error) {
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
	newFiles := interactors.DistinctValues(outFilesBefore, outFilesAfter)
	if len(newFiles) != 2 {
		return "", "", errors.New("Expected 1 new output filent. A race condition exists")
	}
	return newFiles[0], newFiles[1], nil
}

func diffTarget(pyScript string, config ent.ApplicationConfig, target ent.DiffTarget) error {
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
func TransferScriptOutputToDownstream(
	config ent.ApplicationConfig,
	target ent.DiffTarget,
	projectCSVFile, commitCSVFile string,
	common *ent.ManifestFile) error {

	diffRows, commitRows, err := readCSVFiles(projectCSVFile, commitCSVFile)
	if err != nil {
		return err
	}

	manifestFileGroup, err := loadTargetManifests(config, common)
	if err != nil {
		return err
	}
	analyzedDiffRows, analyzedCommitRows := interactors.ApplyApplicationMutations(
		interactors.AppProcessingParameters{
			DiffRows:   diffRows,
			CommitRows: commitRows,
			Manifests:  manifestFileGroup,
		},
	)
	return persistEntities(target, analyzedDiffRows, analyzedCommitRows)
}

func loadTargetManifests(config ent.ApplicationConfig, common *ent.ManifestFile) (*ent.ManifestFileGroup, error) {
	var upstream, downstream ent.ManifestFile
	dirToLoadAddress := map[string]*ent.ManifestFile{
		"upstream":   &upstream,
		"downstream": &downstream,
	}

	for dir, addr := range dirToLoadAddress {
		if err := filesystem.ReadXMLAsEntity(
			filepath.Join(config.OutputDirectory, dir, ".repo/manifest.xml"),
			addr,
		); err != nil {
			return nil, err
		}
	}

	return &ent.ManifestFileGroup{
		Common:     *common,
		Upstream:   upstream,
		Downstream: downstream,
	}, nil
}

func readCSVFiles(projectCSVFile, commitCSVFile string) ([]ent.DiffRow, []ent.CommitRow, error) {
	diffRows, err := csvFileToDiffRows(projectCSVFile)
	if err != nil {
		return nil, nil, errors.Wrap(err, "Error converting CSV file to entities")
	}
	commitRows, err := CSVFileToCommitRows(commitCSVFile)
	if err != nil {
		return nil, nil, errors.Wrap(err, "Error converting CSV file to entities")
	}
	return diffRows, commitRows, nil
}

func persistEntities(target ent.DiffTarget, diffRows []ent.AnalyzedDiffRow, commitRows []ent.AnalyzedCommitRow) error {
	sourceRepo, err := repositories.NewSourceRepository()
	if err != nil {
		return errors.Wrap(err, "Error initializing Source Repository")
	}
	mappedTarget, err := sourceRepo.DiffTargetToMapped(target)
	if err != nil {
		return errors.Wrap(err, "Error mapping diff targets; a race condition is possible")
	}
	err = persistDiffRowsDownstream(mappedTarget, diffRows)
	if err != nil {
		return errors.Wrap(err, "Error persisting diff rows")
	}

	return MaybeNullObjectCommitRepository(
		mappedTarget,
	).InsertCommitRows(
		commitRows,
	)
}

func csvFileToDiffRows(csvFile string) ([]ent.DiffRow, error) {
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

func toDiffRows(entities []interface{}) ([]ent.DiffRow, error) {
	diffRows := make([]ent.DiffRow, len(entities))
	for i, entity := range entities {
		diffRow, ok := entity.(*ent.DiffRow)
		if !ok {
			return nil, errors.New("Error casting to DiffRow")
		}
		diffRows[i] = *diffRow
	}
	return diffRows, nil
}

func CSVFileToCommitRows(csvFile string) ([]ent.CommitRow, error) {
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

func toCommitRows(entities []interface{}) ([]ent.CommitRow, error) {
	commitRows := make([]ent.CommitRow, len(entities))
	for i, entity := range entities {
		commitRow, ok := entity.(*ent.CommitRow)
		if !ok {
			return nil, errors.New("Error casting to CommitRow")
		}
		commitRows[i] = *commitRow
	}
	return commitRows, nil
}

func persistDiffRowsDownstream(mappedTarget ent.MappedDiffTarget, diffRows []ent.AnalyzedDiffRow) error {
	p, err := repositories.NewProjectRepository(mappedTarget)
	if err != nil {
		return errors.Wrap(err, "Error instantiating a new project repository")
	}
	err = p.InsertDiffRows(diffRows)
	if err != nil {
		return errors.Wrap(err, "Error inserting rows from controller")
	}
	return nil
}
