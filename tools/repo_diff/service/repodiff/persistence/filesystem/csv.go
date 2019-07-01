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

func WriteCSVToFile(headerCols []string, rowsOfCols [][]string, filepath string) error {
	file, err := os.Create(filepath)
	if err != nil {
		return err
	}
	defer file.Close()

	writer := csv.NewWriter(file)
	defer writer.Flush()

	if err := writer.Write(headerCols); err != nil {
		return err
	}
	return writer.WriteAll(rowsOfCols)
}
