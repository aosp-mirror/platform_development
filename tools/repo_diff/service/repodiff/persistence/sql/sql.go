package sql

import (
	"database/sql"
	"fmt"
	"runtime"
	"sync"

	"github.com/GoogleCloudPlatform/cloudsql-proxy/proxy/dialers/mysql"
	"github.com/pkg/errors"

	"repodiff/constants"
)

var mux sync.Mutex
var db *sql.DB

type handleRowFn func(*sql.Rows)

func newDBConnectionPool() (*sql.DB, error) {
	cfg := mysql.Cfg(
		constants.GetConfigVar("GCP_DB_INSTANCE_CONNECTION_NAME"),
		constants.GetConfigVar("GCP_DB_USER"),
		constants.GetConfigVar("GCP_DB_PASSWORD"),
	)
	cfg.DBName = constants.GetConfigVar("GCP_DB_NAME")
	return mysql.DialCfg(cfg)
}

func maxParallelism() int {
	maxProcs := runtime.GOMAXPROCS(0)
	numCPU := runtime.NumCPU()
	if maxProcs < numCPU {
		return maxProcs
	}
	return numCPU
}

func GetDBConnectionPool() (*sql.DB, error) {
	if db != nil {
		return db, nil
	}
	mux.Lock()
	defer mux.Unlock()

	// check, lock, check; redundant check for thread safety
	if db != nil {
		return db, nil
	}
	var err error
	db, err = newDBConnectionPool()
	if err != nil {
		return nil, err
	}
	connections := maxParallelism()

	// unless explicitly specified, the default connection pool size is unlimited
	db.SetMaxOpenConns(connections)

	// unless explicitly specified, the default is 0 where idle connections are immediately closed
	db.SetMaxIdleConns(connections)
	return db, nil
}

func SingleTransactionInsert(db *sql.DB, insertQuery string, rowsOfCols [][]interface{}) error {
	tx, err := db.Begin()
	if err != nil {
		return errors.Wrap(err, "Error starting transaction")
	}
	stmt, err := tx.Prepare(insertQuery)
	if err != nil {
		return errors.Wrap(err, "Error preparing statement")
	}
	defer stmt.Close()

	for _, cols := range rowsOfCols {
		_, err = stmt.Exec(
			cols...,
		)
		if err != nil {
			tx.Rollback()
			return errors.Wrap(err, "Error inserting values")
		}
	}
	err = tx.Commit()
	if err != nil {
		tx.Rollback()
		return errors.Wrap(
			err,
			"Error committing transaction",
		)
	}
	return nil
}

func Select(db *sql.DB, rowHandler handleRowFn, query string, args ...interface{}) error {
	rows, err := db.Query(
		query,
		args...,
	)
	if err != nil {
		return err
	}
	defer rows.Close()

	for rows.Next() {
		rowHandler(rows)
	}
	if err = rows.Err(); err != nil {
		return err
	}
	return nil
}

func TruncateTable(db *sql.DB, tableName string) error {
	_, err := db.Exec(
		fmt.Sprintf("TRUNCATE TABLE %s", tableName),
	)
	return err
}
