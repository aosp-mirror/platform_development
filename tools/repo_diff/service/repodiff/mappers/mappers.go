package mappers

import (
	"crypto/sha256"
	"database/sql"
	"encoding/hex"
	"fmt"
	"strconv"

	"github.com/pkg/errors"
	"github.com/satori/go.uuid"

	"repodiff/constants"
	e "repodiff/entities"
	"repodiff/utils"
)

const expectedDiffRowLen = 9
const expectedCommitRowLen = 5

func CSVLineToDiffRow(csvColumns []string) (*e.DiffRow, error) {
	if len(csvColumns) != expectedDiffRowLen {
		return nil, errors.New(fmt.Sprintf("Got %d columns but expected %d", len(csvColumns), expectedDiffRowLen))
	}
	intVals, err := batchToInts(csvColumns[4:]...)
	if err != nil {
		return nil, err
	}
	diffStatus, err := constants.GetStatusEnum(csvColumns[3])
	if err != nil {
		return nil, err
	}

	return &e.DiffRow{
		Date:                 csvColumns[0],
		DownstreamProject:    csvColumns[1],
		UpstreamProject:      csvColumns[2],
		DiffStatus:           diffStatus,
		FilesChanged:         intVals[0],
		LineInsertions:       intVals[1],
		LineDeletions:        intVals[2],
		LineChanges:          intVals[3],
		CommitsNotUpstreamed: intVals[4],
		DBInsertTimestamp:    0,
	}, nil
}

func CSVLineToCommitRow(csvColumns []string) (*e.CommitRow, error) {
	if len(csvColumns) != expectedCommitRowLen {
		return nil, errors.New(fmt.Sprintf("Got %d columns but expected %d", len(csvColumns), expectedCommitRowLen))
	}
	return &e.CommitRow{
		Date:              csvColumns[0],
		Commit:            csvColumns[1],
		DownstreamProject: csvColumns[2],
		Author:            csvColumns[3],
		Subject:           csvColumns[4],
	}, nil
}

func batchToInts(intStrings ...string) ([]int, error) {
	ints := make([]int, len(intStrings))
	for i, val := range intStrings {
		var err error
		ints[i], err = strconv.Atoi(val)
		if err != nil {
			return nil, errors.Wrap(err, fmt.Sprintf("Could not convert from %s", val))
		}
	}
	return ints, nil
}

func diffRowToDenormalizedCols(d e.DiffRow, rowIndex int) []interface{} {
	return []interface{}{
		rowIndex,
		d.Date,
		d.DownstreamProject,
		d.UpstreamProject,
		constants.StatusToDisplay[d.DiffStatus],
		d.FilesChanged,
		d.LineInsertions,
		d.LineDeletions,
		d.LineChanges,
		d.CommitsNotUpstreamed,
	}
}

func commitRowToDenormalizedCols(commitRow e.CommitRow, rowIndex int) []interface{} {
	return []interface{}{
		rowIndex,
		commitRow.Commit,
		commitRow.DownstreamProject,
		commitRow.Author,
		commitRow.Subject,
		GetAuthorTechArea(commitRow.Author),
	}
}

func diffRowToPersistCols(d e.DiffRow, uuidBytes string, timestamp int64, rowIndex int) []interface{} {
	return []interface{}{
		timestamp,
		uuidBytes,
		rowIndex,
		d.DownstreamProject,
		d.UpstreamProject,
		d.DiffStatus,
		d.FilesChanged,
		d.LineInsertions,
		d.LineDeletions,
		d.LineChanges,
		d.CommitsNotUpstreamed,
	}
}

func commitRowToPersistCols(c e.CommitRow, uuidBytes string, timestamp int64, rowIndex int) []interface{} {
	return []interface{}{
		timestamp,
		uuidBytes,
		rowIndex,
		c.Commit,
		c.DownstreamProject,
		c.Author,
		c.Subject,
	}
}

func DiffRowsToPersistCols(diffRows []e.DiffRow) [][]interface{} {
	uid := uuid.NewV4()
	ts := utils.TimestampSeconds()

	rows := make([][]interface{}, len(diffRows))
	for i, diffRow := range diffRows {
		rows[i] = diffRowToPersistCols(
			diffRow,
			string(uid.Bytes()),
			ts,
			i,
		)
	}
	return rows
}

func DiffRowsToDenormalizedCols(diffRows []e.DiffRow) [][]interface{} {
	rows := make([][]interface{}, len(diffRows))
	for i, diffRow := range diffRows {
		rows[i] = diffRowToDenormalizedCols(
			diffRow,
			i,
		)
	}
	return rows
}

func CommitRowsToDenormalizedCols(commitRows []e.CommitRow) [][]interface{} {
	rows := make([][]interface{}, len(commitRows))
	for i, commitRow := range commitRows {
		rows[i] = commitRowToDenormalizedCols(
			commitRow,
			i,
		)
	}
	return rows
}

func CommitRowsToTopCommitter(commitRows []e.CommitRow) [][]interface{} {
	return aggregateToTopCommitterRows(
		groupCommitRowsByAuthor(commitRows),
	)
}

func aggregateToTopCommitterRows(authorToRows map[string][]e.CommitRow) [][]interface{} {
	var rows [][]interface{}
	surrogateID := 1
	for author, rowsThisAuthor := range authorToRows {
		columns := []interface{}{
			surrogateID,
			author,
			len(rowsThisAuthor),
			0, // line changes 0 for now, try to support later
			GetAuthorTechArea(author),
		}
		rows = append(
			rows,
			columns,
		)
		surrogateID++
	}
	return rows
}

func groupCommitRowsByAuthor(commitRows []e.CommitRow) map[string][]e.CommitRow {
	authorToRows := make(map[string][]e.CommitRow)
	for _, c := range commitRows {
		key := c.Author
		if _, keyExists := authorToRows[key]; !keyExists {
			authorToRows[key] = nil
		}
	}

	for _, c := range commitRows {
		key := c.Author
		authorToRows[key] = append(
			authorToRows[key],
			c,
		)
	}
	return authorToRows
}

func DiffRowsToAggregateChangesOverTime(diffRows []e.DiffRow) [][]interface{} {
	if len(diffRows) == 0 {
		return nil
	}
	cols := []interface{}{
		utils.TimestampToDatastudioDatetime(diffRows[0].DBInsertTimestamp),
		getSumOfAttribute(
			diffRows,
			func(d e.DiffRow) int {
				if d.DiffStatus == constants.StatusModified {
					return 1
				}
				return 0
			},
		),
		getSumOfAttribute(
			diffRows,
			func(d e.DiffRow) int {
				return d.LineChanges
			},
		),
		getSumOfAttribute(
			diffRows,
			func(d e.DiffRow) int {
				return d.FilesChanged
			},
		),
	}
	rows := [][]interface{}{
		cols,
	}
	return rows
}

func getSumOfAttribute(diffRows []e.DiffRow, getAttr func(e.DiffRow) int) int {
	var sum int
	for _, d := range diffRows {
		sum += getAttr(d)
	}
	return sum
}

func CommitRowsToPersistCols(commitRows []e.CommitRow) [][]interface{} {
	uid := uuid.NewV4()
	ts := utils.TimestampSeconds()

	rows := make([][]interface{}, len(commitRows))
	for i, commitRow := range commitRows {
		rows[i] = commitRowToPersistCols(
			commitRow,
			string(uid.Bytes()),
			ts,
			i,
		)
	}
	return rows
}

func SQLRowToDiffRow(iterRow *sql.Rows) (e.DiffRow, error) {
	var d e.DiffRow
	var uuidBytes []byte
	var rowIndex int
	err := iterRow.Scan(
		&d.DBInsertTimestamp,
		&uuidBytes,
		&rowIndex,
		&d.DownstreamProject,
		&d.UpstreamProject,
		&d.DiffStatus,
		&d.FilesChanged,
		&d.LineInsertions,
		&d.LineDeletions,
		&d.LineChanges,
		&d.CommitsNotUpstreamed,
	)
	d.Date = utils.TimestampToDate(d.DBInsertTimestamp)
	return d, err
}

func SQLRowToCommitRow(iterRow *sql.Rows) (e.CommitRow, error) {
	var c e.CommitRow
	var uuidBytes []byte
	var rowIndex int
	var timestamp int64
	err := iterRow.Scan(
		&timestamp,
		&uuidBytes,
		&rowIndex,
		&c.Commit,
		&c.DownstreamProject,
		&c.Author,
		&c.Subject,
	)
	c.Date = utils.TimestampToDate(timestamp)
	return c, err
}

// SBL needs test coverage
func PrependMappedDiffTarget(target e.MappedDiffTarget, rowsOfCols [][]interface{}) [][]interface{} {
	remapped := make([][]interface{}, len(rowsOfCols))
	prefix := []interface{}{
		target.UpstreamTarget,
		target.DownstreamTarget,
	}
	for i, row := range rowsOfCols {
		remapped[i] = append(
			prefix,
			row...,
		)
	}
	return remapped
}

func AppendDiffTarget(target e.DiffTarget, rowsOfCols [][]interface{}) [][]interface{} {
	remapped := make([][]interface{}, len(rowsOfCols))
	suffix := []interface{}{
		target.Upstream.URL,
		target.Upstream.Branch,
		target.Downstream.URL,
		target.Downstream.Branch,
	}
	for i, row := range rowsOfCols {
		remapped[i] = append(
			row,
			suffix...,
		)
	}
	return remapped
}

func SHA256HexDigest(s string) string {
	byteArray := sha256.Sum256([]byte(s))
	return hex.EncodeToString(
		byteArray[:],
	)
}

func GetAuthorTechArea(authorEMail string) string {
	techAreaIndex, ok := constants.AuthorHashToTechIndex[SHA256HexDigest(authorEMail)]
	if !ok {
		return constants.TechAreaDisplay[constants.Unknown]
	}
	return constants.TechAreaDisplay[techAreaIndex]
}
