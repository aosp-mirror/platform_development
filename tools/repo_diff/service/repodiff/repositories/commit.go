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

type commit struct {
	db     *sql.DB
	target e.MappedDiffTarget
}

func (c commit) InsertCommitRows(commitRows []e.CommitRow) error {
	return errors.Wrap(
		repoSQL.SingleTransactionInsert(
			c.db,
			`INSERT INTO project_commit (
				upstream_target_id,
				downstream_target_id,
				timestamp,
				uuid,
				row_index,
				commit_,
				downstream_project,
				author,
				subject
			) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
			mappers.PrependMappedDiffTarget(
				c.target,
				mappers.CommitRowsToPersistCols(commitRows),
			),
		),
		"Error inserting rows into project_commit",
	)
}

func (c commit) GetMostRecentOuterKey() (int64, uuid.UUID, error) {
	var timestamp int64
	var uuidBytes []byte
	err := c.db.QueryRow(
		`SELECT timestamp, uuid FROM project_commit WHERE upstream_target_id = ? AND downstream_target_id = ? AND timestamp=(
			SELECT MAX(timestamp) FROM project_commit WHERE upstream_target_id = ? AND downstream_target_id = ?
		) LIMIT 1`,
		c.target.UpstreamTarget,
		c.target.DownstreamTarget,
		c.target.UpstreamTarget,
		c.target.DownstreamTarget,
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

func (c commit) GetMostRecentCommits() ([]e.CommitRow, error) {
	timestamp, uid, err := c.GetMostRecentOuterKey()
	if err == sql.ErrNoRows {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	var errMapping error

	var commitRows []e.CommitRow
	errSelect := repoSQL.Select(
		c.db,
		func(row *sql.Rows) {
			if errMapping != nil {
				return
			}
			c, err := mappers.SQLRowToCommitRow(row)
			errMapping = err
			commitRows = append(
				commitRows,
				c,
			)
		},
		`SELECT
		  timestamp,
			uuid,
			row_index,
			commit_,
			downstream_project,
			author,
			subject
		FROM project_commit
		WHERE
		  upstream_target_id = ?
			AND downstream_target_id = ?
			AND timestamp = ?
			AND uuid = ?`,
		c.target.UpstreamTarget,
		c.target.DownstreamTarget,
		timestamp,
		string(uid.Bytes()),
	)
	if errSelect != nil {
		return nil, errSelect
	}
	if errMapping != nil {
		return nil, errMapping
	}
	return commitRows, nil
}

func NewCommitRepository(target e.MappedDiffTarget) (commit, error) {
	db, err := repoSQL.GetDBConnectionPool()
	return commit{
		db:     db,
		target: target,
	}, errors.Wrap(err, "Could not establish a database connection")
}
