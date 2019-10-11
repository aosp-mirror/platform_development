package sql

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestConnection(t *testing.T) {
	db, err := GetDBConnectionPool()
	assert.Equal(t, nil, err, "Database interface error should not be nil")
	err = db.Ping()
	assert.Equal(t, nil, err, "No error should exist pinging the database")
}
