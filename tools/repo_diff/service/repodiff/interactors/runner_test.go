package interactors

import (
	"testing"

	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
)

func funcWithErr() error {
	return errors.New("This is a private residence, man")
}

func funcNoErr() error {
	return nil
}

func TestExecuteFunctionsAsyncErrExists(t *testing.T) {
	err := NewTaskRunner().ExecuteFunctionsAsync(
		[]func() error{
			funcNoErr,
			funcWithErr,
			funcNoErr,
		},
	)
	assert.NotEqual(t, nil, err, "Error should exist")
}

func TestExecuteFunctionsAsyncNoErr(t *testing.T) {
	err := NewTaskRunner().ExecuteFunctionsAsync(
		[]func() error{
			funcNoErr,
			funcNoErr,
			funcNoErr,
		},
	)
	assert.Equal(t, nil, err, "Error should not exist")
}
