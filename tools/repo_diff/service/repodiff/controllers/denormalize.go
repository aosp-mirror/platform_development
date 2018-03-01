package controllers

import (
	"repodiff/interactors"
	"repodiff/repositories"
)

func DenormalizeData() error {
	return interactors.NewTaskRunner().ExecuteFunctionsAsync(
		[]func() error{
			denormalizeViewRecentProject,
			denormalizeViewChangesOverTime,
		},
	)
}

func denormalizeViewRecentProject() error {
	denormalizeRepo, err := repositories.NewDenormalizerRepository()
	if err != nil {
		return err
	}

	projectRepo, err := repositories.NewProjectRepository()
	if err != nil {
		return err
	}

	diffRows, err := projectRepo.GetMostRecentDifferentials()
	if err != nil {
		return err
	}
	return denormalizeRepo.DenormalizeToRecentView(diffRows)
}

func denormalizeViewChangesOverTime() error {
	denormalizeRepo, err := repositories.NewDenormalizerRepository()
	if err != nil {
		return err
	}

	projectRepo, err := repositories.NewProjectRepository()
	if err != nil {
		return err
	}

	diffRows, err := projectRepo.GetMostRecentDifferentials()
	if err != nil {
		return err
	}
	return denormalizeRepo.DenormalizeToChangesOverTime(diffRows)
}
