package filesystem

import (
	"bufio"
	"encoding/csv"
	"fmt"
	"io"
	"os"

	"github.com/pkg/errors"
)

type lineHandler func(csvColumns []string)

func GenerateCSVLines(filePath string, handler lineHandler) error {
	csvFile, err := os.Open(filePath)
	if err != nil {
		return errors.Wrap(
			err,
			fmt.Sprintf("Could not open %s", filePath),
		)
	}
	reader := csv.NewReader(
		bufio.NewReader(csvFile),
	)

	isFirstLine := true
	for {
		line, err := reader.Read()
		if err == io.EOF {
			break
		} else if err != nil {
			return errors.Wrap(
				err,
				fmt.Sprintf("Could not read line from file %s", filePath),
			)
		}
		if !isFirstLine {
			handler(line)
		}
		isFirstLine = false
	}
	return nil
}
