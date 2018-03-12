package repositories

import (
	"database/sql"

	"github.com/pkg/errors"
	"github.com/satori/go.uuid"

	"repodiff/constants"
	e "repodiff/entities"
	"repodiff/mappers"
	repoSQL "repodiff/persistence/sql"
)

type project struct {
	db *sql.DB
}

func (p project) InsertDiffRows(diffRows []e.DiffRow) error {
	return errors.Wrap(
		repoSQL.SingleTransactionInsert(
			p.db,
			`INSERT INTO project_differential (
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
				commits_not_upstreamed
			) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
			mappers.DiffRowsToPersistCols(diffRows),
		),
		"Error inserting rows into project_differential",
	)
}

func (p project) GetMostRecentOuterKey() (int64, uuid.UUID, error) {
	var timestamp int64
	var uuidBytes []byte
	err := p.db.QueryRow(
		`SELECT timestamp, uuid FROM project_differential WHERE timestamp=(
			SELECT MAX(timestamp) FROM project_differential
		) LIMIT 1`,
	).Scan(
		&timestamp,
		&uuidBytes,
	)
	if err != nil {
		return 0, constants.NullUUID(), errors.Wrap(err, "Error querying latest timestamp")
	}
	u, err := uuid.FromBytes(uuidBytes)
	if err != nil {
		return 0, constants.NullUUID(), errors.Wrap(err, "Error casting string to UUID")
	}
	return timestamp, u, nil
}

func (p project) GetMostRecentDifferentials() ([]e.DiffRow, error) {
	timestamp, uid, err := p.GetMostRecentOuterKey()
	if err != nil {
		return nil, err
	}
	var errMapping error

	var diffRows []e.DiffRow
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
		"SELECT * FROM project_differential WHERE timestamp = ? AND uuid = ?",
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

func NewProjectRepository() (project, error) {
	db, err := repoSQL.GetDBConnectionPool()
	return project{
		db: db,
	}, errors.Wrap(err, "Could not establish a database connection")
}
