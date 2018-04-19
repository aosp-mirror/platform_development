package repositories

import (
	"database/sql"
	"strings"

	"github.com/pkg/errors"
	"github.com/satori/go.uuid"

	"repodiff/constants"
	e "repodiff/entities"
	"repodiff/interactors"
	"repodiff/mappers"
	repoSQL "repodiff/persistence/sql"
	"repodiff/utils"
)

type Commit struct {
	db                 *sql.DB
	target             e.MappedDiffTarget
	timestampGenerator func() e.RepoTimestamp
}

func (c Commit) WithTimestampGenerator(t func() e.RepoTimestamp) Commit {
	return Commit{
		db:                 c.db,
		target:             c.target,
		timestampGenerator: t,
	}
}

func (c Commit) InsertCommitRows(commitRows []e.AnalyzedCommitRow) error {
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
				subject,
				project_type
			) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
			mappers.PrependMappedDiffTarget(
				c.target,
				mappers.CommitRowsToPersistCols(commitRows, c.timestampGenerator()),
			),
		),
		"Error inserting rows into project_commit",
	)
}

func (c Commit) GetMostRecentOuterKey() (int64, uuid.UUID, error) {
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

func (c Commit) GetMostRecentCommits() ([]e.AnalyzedCommitRow, error) {
	timestamp, uid, err := c.GetMostRecentOuterKey()
	if err == sql.ErrNoRows {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	var errMapping error

	var commitRows []e.AnalyzedCommitRow
	var commitCursor e.AnalyzedCommitRow
	errSelect := repoSQL.Select(
		c.db,
		func(row *sql.Rows) {
			if err := interactors.ExistingErrorOr(
				errMapping,
				func() error {
					commitCursor, err = mappers.SQLRowToCommitRow(row)
					return err
				},
			); err != nil {
				errMapping = err
				return
			}
			commitRows = append(
				commitRows,
				commitCursor,
			)
		},
		`SELECT
		  timestamp,
			uuid,
			row_index,
			commit_,
			downstream_project,
			author,
			subject,
			project_type
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
	if err := interactors.AnyError(errSelect, errMapping); err != nil {
		return nil, err
	}
	return commitRows, nil
}

func (c Commit) GetFirstSeenTimestamp(commitHashes []string) (map[string]e.RepoTimestamp, error) {
	if len(commitHashes) == 0 {
		return map[string]e.RepoTimestamp{}, nil
	}
	commitToTimestamp := make(map[string]e.RepoTimestamp, len(commitHashes))
	var commitCursor string
	var timestampCursor e.RepoTimestamp
	var errMapping error

	errSelect := repoSQL.Select(
		c.db,
		func(row *sql.Rows) {
			if err := interactors.ExistingErrorOr(
				errMapping,
				func() error {
					return row.Scan(
						&commitCursor,
						&timestampCursor,
					)
				},
			); err != nil {
				errMapping = err
				return
			}
			commitToTimestamp[commitCursor] = timestampCursor
		},
		`SELECT commit_, MIN(timestamp)
			FROM project_commit
				WHERE upstream_target_id = ?
					AND downstream_target_id = ?
					AND commit_ IN(?)
				GROUP BY commit_
		`,
		c.target.UpstreamTarget,
		c.target.DownstreamTarget,
		strings.Join(commitHashes, ", "),
	)
	if err := interactors.AnyError(errSelect, errMapping); err != nil {
		return nil, err
	} else if len(commitToTimestamp) != len(commitHashes) {
		return nil, errors.New("Not all input commit hashes exist")
	}
	return commitToTimestamp, nil
}

func NewCommitRepository(target e.MappedDiffTarget) (Commit, error) {
	db, err := repoSQL.GetDBConnectionPool()
	return Commit{
		db:                 db,
		target:             target,
		timestampGenerator: utils.TimestampSeconds,
	}, errors.Wrap(err, "Could not establish a database connection")
}
