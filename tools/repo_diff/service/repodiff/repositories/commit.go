package repositories

import (
	"database/sql"

	"github.com/pkg/errors"

	e "repodiff/entities"
	"repodiff/mappers"
	repoSQL "repodiff/persistence/sql"
)

type commit struct {
	db *sql.DB
}

func (c commit) InsertCommitRows(commitRows []e.CommitRow) error {
	return errors.Wrap(
		repoSQL.SingleTransactionInsert(
			c.db,
			`INSERT INTO project_commit (
				timestamp,
				uuid,
				row_index,
				commit_,
				downstream_project,
				author,
				subject
			) VALUES (?, ?, ?, ?, ?, ?, ?)`,
			mappers.CommitRowsToPersistCols(commitRows),
		),
		"Error inserting rows into project_commit",
	)
}

func NewCommitRepository() (commit, error) {
	db, err := repoSQL.GetDBConnectionPool()
	return commit{
		db: db,
	}, errors.Wrap(err, "Could not establish a database connection")
}
