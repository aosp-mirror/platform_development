package repositories

import (
	"database/sql"
	"fmt"

	"github.com/pkg/errors"

	ent "repodiff/entities"
	"repodiff/mappers"
	repoSQL "repodiff/persistence/sql"
)

type GlobalDenormalizer struct {
	db *sql.DB
}

type ScopedDenormalizer struct {
	db           *sql.DB
	target       ent.DiffTarget
	mappedTarget ent.MappedDiffTarget
}

func (g GlobalDenormalizer) DenormalizeToTopCommitter() error {
	table := "denormalized_view_top_committer"
	if _, err := g.db.Exec(
		fmt.Sprintf(
			"TRUNCATE TABLE %s",
			table,
		),
	); err != nil {
		return err
	}
	_, err := g.db.Exec(
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
				) (
					SELECT
						upstream_target_id,
						downstream_target_id,
						@rn:=@rn+1 AS surrogate_id,
						committer,
						commits,
						line_changes,
						tech_area,
						upstream_url,
						upstream_branch,
						downstream_url,
						downstream_branch
					FROM (
						SELECT upstream_target_id,
							downstream_target_id,
							author as committer,
							tech_area,
							COUNT(*) AS commits,
							SUM(0) AS line_changes,
							upstream_url,
							upstream_branch,
							downstream_url,
							downstream_branch
						FROM denormalized_view_recent_commit GROUP BY
							author,
							tech_area,
							upstream_target_id,
							downstream_target_id,
							upstream_url,
							upstream_branch,
							downstream_url,
							downstream_branch ORDER BY upstream_target_id,
							downstream_target_id
						) t1,
						(SELECT @rn:=0) t2
					)`,
			table,
		),
	)
	return err
}

func (g GlobalDenormalizer) DenormalizeToTopTechArea() error {
	table := "denormalized_view_top_tech_area"
	if _, err := g.db.Exec(
		fmt.Sprintf(
			"TRUNCATE TABLE %s",
			table,
		),
	); err != nil {
		return err
	}
	_, err := g.db.Exec(
		fmt.Sprintf(
			`INSERT INTO %s (
					upstream_target_id,
					downstream_target_id,
					surrogate_id,
					tech_area,
					commits,
					line_changes,
					upstream_url,
					upstream_branch,
					downstream_url,
					downstream_branch
				) (
					SELECT
						upstream_target_id,
						downstream_target_id,
						@rn:=@rn+1 AS surrogate_id,
						tech_area,
						commits,
						line_changes,
						upstream_url,
						upstream_branch,
						downstream_url,
						downstream_branch FROM (
							SELECT
								upstream_target_id,
								downstream_target_id,
								tech_area,
								COUNT(*) AS commits,
								SUM(0) AS line_changes,
								upstream_url,
								upstream_branch,
								downstream_url,
								downstream_branch
							FROM denormalized_view_recent_commit GROUP BY
								tech_area,
								upstream_target_id,
								downstream_target_id,
								upstream_url,
								upstream_branch,
								downstream_url,
								downstream_branch
							ORDER BY
								upstream_target_id,
								downstream_target_id
						) t1,
						(SELECT @rn:=0) t2
					)`,
			table,
		),
	)
	return err
}

func (s ScopedDenormalizer) DenormalizeToRecentView(diffRows []ent.AnalyzedDiffRow) error {
	table := "denormalized_view_recent_project"
	if err := s.deleteExistingView(table); err != nil {
		return err
	}
	return errors.Wrap(
		repoSQL.SingleTransactionInsert(
			s.db,
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
					project_type,
					upstream_url,
					upstream_branch,
					downstream_url,
					downstream_branch
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
				table,
			),
			s.rowsWithScopedIndices(
				mappers.DiffRowsToDenormalizedCols(diffRows),
			),
		),
		errorMessageForTable(table),
	)
}

func NewGlobalDenormalizerRepository() (GlobalDenormalizer, error) {
	db, err := repoSQL.GetDBConnectionPool()
	return GlobalDenormalizer{
		db: db,
	}, errors.Wrap(err, "Could not establish a database connection")
}

func NewScopedDenormalizerRepository(target ent.DiffTarget, mappedTarget ent.MappedDiffTarget) (ScopedDenormalizer, error) {
	db, err := repoSQL.GetDBConnectionPool()
	return ScopedDenormalizer{
		db:           db,
		target:       cleanedDiffTarget(target),
		mappedTarget: mappedTarget,
	}, errors.Wrap(err, "Could not establish a database connection")
}

func (s ScopedDenormalizer) DenormalizeToChangesOverTime(diffRows []ent.AnalyzedDiffRow) error {
	// This query only inserts a single row into the database.  If it becomes problematic, this
	// could become more efficient without the prepared statement embedded in the SingleTransactionInsert
	// function
	table := "denormalized_view_changes_over_time"
	return errors.Wrap(
		repoSQL.SingleTransactionInsert(
			s.db,
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
			s.rowsWithScopedIndices(
				mappers.DiffRowsToAggregateChangesOverTime(diffRows),
			),
		),
		errorMessageForTable(table),
	)
}

func (s ScopedDenormalizer) DenormalizeToRecentCommits(commitRows []ent.AnalyzedCommitRow, commitToTimestamp map[string]ent.RepoTimestamp) error {
	table := "denormalized_view_recent_commit"
	if err := s.deleteExistingView(table); err != nil {
		return err
	}
	return errors.Wrap(
		repoSQL.SingleTransactionInsert(
			s.db,
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
					project_type,
					first_seen_datastudio_datetime,
					upstream_url,
					upstream_branch,
					downstream_url,
					downstream_branch
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
				table,
			),
			s.rowsWithScopedIndices(
				mappers.CommitRowsToDenormalizedCols(commitRows, commitToTimestamp),
			),
		),
		errorMessageForTable(table),
	)
}

func (s ScopedDenormalizer) deleteExistingView(tableName string) error {
	_, err := s.db.Exec(
		fmt.Sprintf(
			`DELETE FROM %s
			WHERE
				upstream_target_id = ?
				AND downstream_target_id = ?`,
			tableName,
		),
		s.mappedTarget.UpstreamTarget,
		s.mappedTarget.DownstreamTarget,
	)
	return err
}

func (s ScopedDenormalizer) rowsWithScopedIndices(rowsOfCols [][]interface{}) [][]interface{} {
	return mappers.PrependMappedDiffTarget(
		s.mappedTarget,
		mappers.AppendDiffTarget(
			s.target,
			rowsOfCols,
		),
	)
}

func errorMessageForTable(tableName string) string {
	return fmt.Sprintf("Error inserting rows into %s", tableName)
}
