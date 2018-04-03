package controllers

import (
	e "repodiff/entities"
	"repodiff/interactors"
	"repodiff/repositories"
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
	denormalizeRepo, err := repositories.NewDenormalizerRepository(target, mappedTarget)
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
		err := denormalizeDiffRowsForTarget(target)
		if err != nil {
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
	denormalizeRepo, err := repositories.NewDenormalizerRepository(target, mappedTarget)
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
		err := denormalizeCommitRows(target)
		if err != nil {
			return err
		}
	}
	return nil
}

func denormalizeCommitRows(target e.DiffTarget) error {
	mappedTarget, err := getMappedTarget(target)
	if err != nil {
		return err
	}
	denormalizeRepo, err := repositories.NewDenormalizerRepository(target, mappedTarget)
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
	err = denormalizeRepo.DenormalizeToRecentCommits(commitRows)
	if err != nil {
		return err
	}
	return denormalizeRepo.DenormalizeToTopCommitter(commitRows)
}
