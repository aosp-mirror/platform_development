package utils

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestTimestampSeconds(t *testing.T) {
	var oldTimestamp int64 = 1519322647
	newTimestamp := TimestampSeconds()
	assert.True(t, newTimestamp > oldTimestamp, "New timestamp should be greater than fixture")

}

func TestTimestampToDate(t *testing.T) {
	var timestamp int64 = 1519322647
	assert.Equal(t, "2018-02-22", TimestampToDate(timestamp), "Date conversion")
}

func TestTimestampToDatastudioDatetime(t *testing.T) {
	var timestamp int64 = 1519322647
	assert.Equal(t, "2018022210", TimestampToDatastudioDatetime(timestamp), "Datetime conversion")
}
