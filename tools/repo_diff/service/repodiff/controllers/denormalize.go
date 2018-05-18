package controllers

import (
	e "repodiff/entities"
	"repodiff/interactors"
	"repodiff/repositories"
	"repodiff/utils"
)

func DenormalizeData(config e.ApplicationConfig) error {
	return interactors.NewTaskRunner().ExecuteFunctionsAsync(
		[]func() error{
			func() error { return denormalizeViewRecentProject(config) },
			func() error { return denormalizeDiffRows(config) },
			func() error { return denormalizeViewRecentCommit(config) },
		},
	)
}

func denormalizeViewRecentProject(config e.ApplicationConfig) error {
	for _, target := range config.DiffTargets {
		err := denormalizeViewRecentProjectForTarget(target)
		if err != nil {
			return err
		}
	}
	return nil
}

func denormalizeViewRecentProjectForTarget(target e.DiffTarget) error {
	mappedTarget, err := getMappedTarget(target)
	if err != nil {
		return err
	}
	denormalizeRepo, err := repositories.NewScopedDenormalizerRepository(target, mappedTarget)
	if err != nil {
		return err
	}

	projectRepo, err := repositories.NewProjectRepository(mappedTarget)
	if err != nil {
		return err
	}

	diffRows, err := projectRepo.GetMostRecentDifferentials()
	if err != nil {
		return err
	}
	return denormalizeRepo.DenormalizeToRecentView(diffRows)
}

func denormalizeDiffRows(config e.ApplicationConfig) error {
	for _, target := range config.DiffTargets {
		if err := denormalizeDiffRowsForTarget(target); err != nil {
			return err
		}
	}
	return nil
}

func denormalizeDiffRowsForTarget(target e.DiffTarget) error {
	mappedTarget, err := getMappedTarget(target)
	if err != nil {
		return err
	}
	denormalizeRepo, err := repositories.NewScopedDenormalizerRepository(target, mappedTarget)
	if err != nil {
		return err
	}

	projectRepo, err := repositories.NewProjectRepository(mappedTarget)
	if err != nil {
		return err
	}

	diffRows, err := projectRepo.GetMostRecentDifferentials()
	if err != nil {
		return err
	}
	return denormalizeRepo.DenormalizeToChangesOverTime(diffRows)
}

func getMappedTarget(target e.DiffTarget) (e.MappedDiffTarget, error) {
	sourceRepo, err := repositories.NewSourceRepository()
	if err != nil {
		return e.MappedDiffTarget{}, err
	}
	return sourceRepo.DiffTargetToMapped(target)
}

func denormalizeViewRecentCommit(config e.ApplicationConfig) error {
	for _, target := range config.DiffTargets {
		if err := denormalizeCommitRows(target); err != nil {
			return err
		}
	}
	return denormalizeViewRecentCommitGlobal()
}

func denormalizeViewRecentCommitGlobal() error {
	denormalizeRepo, err := repositories.NewGlobalDenormalizerRepository()
	if err != nil {
		return err
	}
	if err := denormalizeRepo.DenormalizeToTopCommitter(); err != nil {
		return err
	}
	return denormalizeRepo.DenormalizeToTopTechArea()
}

func denormalizeCommitRows(target e.DiffTarget) error {
	mappedTarget, err := getMappedTarget(target)
	if err != nil {
		return err
	}
	denormalizeRepo, err := repositories.NewScopedDenormalizerRepository(target, mappedTarget)
	if err != nil {
		return err
	}

	commitRepo, err := repositories.NewCommitRepository(mappedTarget)
	if err != nil {
		return err
	}
	commitRows, err := commitRepo.GetMostRecentCommits()
	if err != nil {
		return err
	}
	commitToTimestamp, err := MaybeNullObjectCommitRepository(
		mappedTarget,
	).GetFirstSeenTimestamp(
		extractCommitHashes(commitRows),
		utils.TimestampSeconds(),
	)
	if err != nil {
		return err
	}

	return denormalizeRepo.DenormalizeToRecentCommits(commitRows, commitToTimestamp)
}

func extractCommitHashes(commitRows []e.AnalyzedCommitRow) []string {
	hashes := make([]string, len(commitRows))
	for i, row := range commitRows {
		hashes[i] = row.Commit
	}
	return hashes
}
