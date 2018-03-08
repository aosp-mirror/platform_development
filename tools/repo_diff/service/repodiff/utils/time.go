package utils

import (
	"fmt"
	t "time"
)

func TimestampSeconds() int64 {
	return t.Now().Unix()
}

func TimestampToDate(timestamp int64) string {
	year, month, day := t.Unix(timestamp, 0).Date()
	return fmt.Sprintf("%04d-%02d-%02d", year, month, day)
}

// Formats a timestamp into a datetime acceptable for MySQL
func TimestampToDatastudioDatetime(timestamp int64) string {
	asTime := t.Unix(timestamp, 0)
	return fmt.Sprintf(
		"%04d%02d%02d%02d",
		asTime.Year(),
		asTime.Month(),
		asTime.Day(),
		asTime.Hour(),
	)
}
