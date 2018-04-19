package repositories

import (
	"database/sql"

	"github.com/pkg/errors"
	"github.com/satori/go.uuid"

	"repodiff/constants"
	e "repodiff/entities"
	"repodiff/mappers"
	repoSQL "repodiff/persistence/sql"
	"repodiff/utils"
)

type project struct {
	db                 *sql.DB
	target             e.MappedDiffTarget
	timestampGenerator func() e.RepoTimestamp
}

func (p project) InsertDiffRows(diffRows []e.AnalyzedDiffRow) error {
	return errors.Wrap(
		repoSQL.SingleTransactionInsert(
			p.db,
			`INSERT INTO project_differential (
				upstream_target_id,
				downstream_target_id,
				timestamp,
				uuid,
				row_index,
				downstream_project,
				upstream_project,
				status,
				files_changed,
				line_insertions,
				line_deletions,
				line_changes,
				commits_not_upstreamed,
				project_type
			) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
			mappers.PrependMappedDiffTarget(
				p.target,
				mappers.DiffRowsToPersistCols(diffRows, p.timestampGenerator()),
			),
		),
		"Error inserting rows into project_differential",
	)
}

func (p project) GetMostRecentOuterKey() (int64, uuid.UUID, error) {
	var timestamp int64
	var uuidBytes []byte
	err := p.db.QueryRow(
		`SELECT timestamp, uuid FROM project_differential WHERE upstream_target_id = ? AND downstream_target_id = ? AND timestamp=(
			SELECT MAX(timestamp) FROM project_differential WHERE upstream_target_id = ? AND downstream_target_id = ?
		) LIMIT 1`,
		p.target.UpstreamTarget,
		p.target.DownstreamTarget,
		p.target.UpstreamTarget,
		p.target.DownstreamTarget,
	).Scan(
		&timestamp,
		&uuidBytes,
	)
	if err != nil {
		return 0, constants.NullUUID(), err
	}
	u, err := uuid.FromBytes(uuidBytes)
	if err != nil {
		return 0, constants.NullUUID(), errors.Wrap(err, "Error casting string to UUID")
	}
	return timestamp, u, nil
}

func (p project) GetMostRecentDifferentials() ([]e.AnalyzedDiffRow, error) {
	timestamp, uid, err := p.GetMostRecentOuterKey()
	if err == sql.ErrNoRows {
		return nil, nil
	}
	if err != nil {
		// TODO this doesn't handle empty case properly
		return nil, err
	}
	var errMapping error

	var diffRows []e.AnalyzedDiffRow
	errSelect := repoSQL.Select(
		p.db,
		func(row *sql.Rows) {
			if errMapping != nil {
				return
			}
			d, err := mappers.SQLRowToDiffRow(row)
			errMapping = err
			diffRows = append(
				diffRows,
				d,
			)
		},
		`SELECT
		  timestamp,
			uuid,
			row_index,
			downstream_project,
			upstream_project,
			status,
			files_changed,
			line_insertions,
			line_deletions,
			line_changes,
			commits_not_upstreamed,
			project_type
		FROM project_differential
		WHERE
		  upstream_target_id = ?
			AND downstream_target_id = ?
			AND timestamp = ?
			AND uuid = ?`,
		p.target.UpstreamTarget,
		p.target.DownstreamTarget,
		timestamp,
		string(uid.Bytes()),
	)
	if errSelect != nil {
		return nil, errSelect
	}
	if errMapping != nil {
		return nil, errMapping
	}
	return diffRows, nil
}

func NewProjectRepository(target e.MappedDiffTarget) (project, error) {
	db, err := repoSQL.GetDBConnectionPool()
	return project{
		db:                 db,
		target:             target,
		timestampGenerator: utils.TimestampSeconds,
	}, errors.Wrap(err, "Could not establish a database connection")
}
