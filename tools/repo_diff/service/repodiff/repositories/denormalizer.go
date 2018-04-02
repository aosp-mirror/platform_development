package repositories

import (
	"database/sql"
	"fmt"

	"github.com/pkg/errors"

	e "repodiff/entities"
	"repodiff/mappers"
	repoSQL "repodiff/persistence/sql"
)

type denormalizer struct {
	db           *sql.DB
	target       e.DiffTarget
	mappedTarget e.MappedDiffTarget
}

func (d denormalizer) DenormalizeToRecentView(diffRows []e.DiffRow) error {
	table := "denormalized_view_recent_project"
	err := d.deleteExistingView(table)
	if err != nil {
		return err
	}
	return errors.Wrap(
		repoSQL.SingleTransactionInsert(
			d.db,
			fmt.Sprintf(
				`INSERT INTO %s (
					upstream_target_id,
					downstream_target_id,
					row_index,
					date,
					downstream_project,
					upstream_project,
					status,
					files_changed,
					line_insertions,
					line_deletions,
					line_changes,
					commits_not_upstreamed,
					upstream_url,
					upstream_branch,
					downstream_url,
					downstream_branch
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
				table,
			),
			d.rowsWithScopedIndices(
				mappers.DiffRowsToDenormalizedCols(diffRows),
			),
		),
		errorMessageForTable(table),
	)
}

func NewDenormalizerRepository(target e.DiffTarget, mappedTarget e.MappedDiffTarget) (denormalizer, error) {
	db, err := repoSQL.GetDBConnectionPool()
	return denormalizer{
		db:           db,
		target:       cleanedDiffTarget(target),
		mappedTarget: mappedTarget,
	}, errors.Wrap(err, "Could not establish a database connection")
}

func (d denormalizer) DenormalizeToChangesOverTime(diffRows []e.DiffRow) error {
	// This query only inserts a single row into the database.  If it becomes problematic, this
	// could become more efficient without the prepared statement embedded in the SingleTransactionInsert
	// function
	table := "denormalized_view_changes_over_time"
	return errors.Wrap(
		repoSQL.SingleTransactionInsert(
			d.db,
			fmt.Sprintf(
				`INSERT IGNORE INTO %s (
					upstream_target_id,
					downstream_target_id,
					datastudio_datetime,
					modified_projects,
					line_changes,
					files_changed,
					upstream_url,
					upstream_branch,
					downstream_url,
					downstream_branch
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
				table,
			),
			d.rowsWithScopedIndices(
				mappers.DiffRowsToAggregateChangesOverTime(diffRows),
			),
		),
		errorMessageForTable(table),
	)
}

func (d denormalizer) DenormalizeToRecentCommits(commitRows []e.CommitRow) error {
	table := "denormalized_view_recent_commit"
	err := d.deleteExistingView(table)
	if err != nil {
		return err
	}
	return errors.Wrap(
		repoSQL.SingleTransactionInsert(
			d.db,
			fmt.Sprintf(
				`INSERT INTO %s (
					upstream_target_id,
					downstream_target_id,
					row_index,
					commit_,
					downstream_project,
					author,
					subject,
					tech_area,
					upstream_url,
					upstream_branch,
					downstream_url,
					downstream_branch
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
				table,
			),
			d.rowsWithScopedIndices(
				mappers.CommitRowsToDenormalizedCols(commitRows),
			),
		),
		errorMessageForTable(table),
	)
}

func (d denormalizer) DenormalizeToTopCommitter(commitRows []e.CommitRow) error {
	table := "denormalized_view_top_committer"
	err := d.deleteExistingView(table)
	if err != nil {
		return err
	}
	return errors.Wrap(
		repoSQL.SingleTransactionInsert(
			d.db,
			fmt.Sprintf(
				`INSERT INTO %s (
					upstream_target_id,
					downstream_target_id,
					surrogate_id,
					committer,
					commits,
					line_changes,
					tech_area,
					upstream_url,
					upstream_branch,
					downstream_url,
					downstream_branch
				) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
				table,
			),
			d.rowsWithScopedIndices(
				mappers.CommitRowsToTopCommitter(commitRows),
			),
		),
		errorMessageForTable(table),
	)
}

func (d denormalizer) deleteExistingView(tableName string) error {
	_, err := d.db.Exec(
		fmt.Sprintf(
			`DELETE FROM %s
			WHERE
				upstream_target_id = ?
				AND downstream_target_id = ?`,
			tableName,
		),
		d.mappedTarget.UpstreamTarget,
		d.mappedTarget.DownstreamTarget,
	)
	return err
}

func (d denormalizer) rowsWithScopedIndices(rowsOfCols [][]interface{}) [][]interface{} {
	return mappers.PrependMappedDiffTarget(
		d.mappedTarget,
		mappers.AppendDiffTarget(
			d.target,
			rowsOfCols,
		),
	)
}

func errorMessageForTable(tableName string) string {
	return fmt.Sprintf("Error inserting rows into %s", tableName)
}
