package utils

import (
	"testing"

	"github.com/stretchr/testify/assert"

	ent "repodiff/entities"
)

func TestTimestampSeconds(t *testing.T) {
	var oldTimestamp ent.RepoTimestamp = 1519322647
	newTimestamp := TimestampSeconds()
	assert.True(t, newTimestamp > oldTimestamp, "New timestamp should be greater than fixture")

}

func TestTimestampToDate(t *testing.T) {
	var timestamp ent.RepoTimestamp = 1519322647
	assert.Equal(t, "2018-02-22", TimestampToDate(timestamp), "Date conversion")
}

func TestTimestampToDataStudioDatetime(t *testing.T) {
	var timestamp ent.RepoTimestamp = 1519322647
	assert.Equal(t, "2018022210", TimestampToDataStudioDatetime(timestamp), "Datetime conversion")
}
