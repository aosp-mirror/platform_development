package controllers

import (
	ent "repodiff/entities"
	"repodiff/repositories"
)

type Committer interface {
	InsertCommitRows(commitRows []ent.AnalyzedCommitRow) error
	GetFirstSeenTimestamp(commitHashes []string, nullTimestamp ent.RepoTimestamp) (map[string]ent.RepoTimestamp, error)
	GetMostRecentCommits() ([]ent.AnalyzedCommitRow, error)
}

func MaybeNullObjectCommitRepository(target ent.MappedDiffTarget) Committer {
	c, err := repositories.NewCommitRepository(target)
	if err != nil {
		return repositories.NewNullObject(err)
	}
	return c
}
