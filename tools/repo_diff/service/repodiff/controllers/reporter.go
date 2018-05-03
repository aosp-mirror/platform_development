package controllers

import (
	"fmt"
	"log"
	"os"
	"path/filepath"

	ent "repodiff/entities"
	"repodiff/mappers"
	"repodiff/persistence/filesystem"
	"repodiff/repositories"
	"repodiff/utils"
)

func GenerateCommitReport(appConfig ent.ApplicationConfig) error {
	for _, target := range appConfig.DiffTargets {
		if err := generateCommitReport(appConfig, target); err != nil {
			return err
		}
	}
	return nil
}

func generateCommitReport(appConfig ent.ApplicationConfig, target ent.DiffTarget) error {
	log.Printf("Generating commit report for (upstream) %s vs (downstream) %s\n", target.Upstream.Branch, target.Downstream.Branch)
	sourceRepo, err := repositories.NewSourceRepository()
	if err != nil {
		return err
	}
	mappedTarget, err := sourceRepo.DiffTargetToMapped(target)
	if err != nil {
		return err
	}
	// TODO export the commitRepository type in order to add the possibility of helpers
	commitRepo, err := repositories.NewCommitRepository(mappedTarget)
	if err != nil {
		return err
	}

	commitRows, err := commitRepo.GetMostRecentCommits()
	if err != nil {
		return err
	}

	dir := filepath.Join(appConfig.OutputDirectory, "reports")
	fname := filepath.Join(dir, filenameForTarget(target))
	os.MkdirAll(dir, os.ModePerm)
	log.Printf("Writing to file %s\n", fname)

	if err := filesystem.WriteCSVToFile(
		mappers.CommitCSVHeader(),
		mappers.CommitEntitiesToCSVRows(commitRows),
		fname,
	); err != nil {
		return err
	}
	return nil
}

func filenameForTarget(target ent.DiffTarget) string {
	return fmt.Sprintf(
		"%s_upstream-%s_vs_downstream-%s.csv",
		utils.TimestampToDate(
			utils.TimestampSeconds(),
		),
		target.Upstream.Branch,
		target.Downstream.Branch,
	)

}
