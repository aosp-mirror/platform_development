package utils

import (
	"fmt"
	t "time"

	ent "repodiff/entities"
)

func TimestampSeconds() ent.RepoTimestamp {
	return ent.RepoTimestamp(t.Now().Unix())
}

func TimestampToDate(timestamp ent.RepoTimestamp) string {
	year, month, day := t.Unix(int64(timestamp), 0).Date()
	return fmt.Sprintf("%04d-%02d-%02d", year, month, day)
}

// Formats a timestamp into a datetime acceptable for MySQL
func TimestampToDataStudioDatetime(timestamp ent.RepoTimestamp) string {
	asTime := t.Unix(int64(timestamp), 0)
	return fmt.Sprintf(
		"%04d%02d%02d%02d",
		asTime.Year(),
		asTime.Month(),
		asTime.Day(),
		asTime.Hour(),
	)
}
