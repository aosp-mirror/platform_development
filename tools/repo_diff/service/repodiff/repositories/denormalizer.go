package repositories

import (
	"database/sql"
	"github.com/pkg/errors"

	e "repodiff/entities"
	"repodiff/mappers"
	repoSQL "repodiff/persistence/sql"
)

type denormalizer struct {
	db *sql.DB
}

func (d denormalizer) DenormalizeToRecentView(diffRows []e.DiffRow) error {
	err := repoSQL.TruncateTable(d.db, "denormalized_view_recent_project")
	if err != nil {
		return err
	}
	return errors.Wrap(
		repoSQL.SingleTransactionInsert(
			d.db,
			`INSERT INTO denormalized_view_recent_project (
				row_index,
				date,
				downstream_project,
				upstream_project,
				status,
				files_changed,
				line_insertions,
				line_deletions,
				line_changes,
				commits_not_upstreamed
			) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
			mappers.DiffRowsToDenormalizedCols(diffRows),
		),
		"Error inserting rows into denormalized_view_recent_project",
	)
}

func NewDenormalizerRepository() (denormalizer, error) {
	db, err := repoSQL.GetDBConnectionPool()
	return denormalizer{
		db: db,
	}, errors.Wrap(err, "Could not establish a database connection")
}

func (d denormalizer) DenormalizeToChangesOverTime(diffRows []e.DiffRow) error {
	// This query only inserts a single row into the database.  If it becomes problematic, this
	// could become more efficient without the prepared statement embedded in the SingleTransactionInsert
	// function
	return errors.Wrap(
		repoSQL.SingleTransactionInsert(
			d.db,
			`INSERT INTO denormalized_view_changes_over_time (
				datastudio_datetime,
				modified_projects,
				line_changes,
				files_changed
			) VALUES (?, ?, ?, ?)`,
			mappers.DiffRowsToAggregateChangesOverTime(diffRows),
		),
		"Error inserting rows into denormalized_view_changes_over_time",
	)
}
