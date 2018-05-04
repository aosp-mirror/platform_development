package interactors

import (
	"errors"
	"testing"

	"github.com/stretchr/testify/assert"
)

var trueErr1 error = errors.New("I had a rough night, and I hate the eagles man")
var trueErr2 error = errors.New("This is what happens when you find a stranger in the Alps")

func TestExistingErrorOrTT(t *testing.T) {
	assert.Equal(
		t,
		trueErr1,
		ExistingErrorOr(
			trueErr1,
			func() error {
				return trueErr2
			},
		),
		"Should return first true",
	)
}

func TestExistingErrorOrTF(t *testing.T) {
	assert.Equal(
		t,
		trueErr1,
		ExistingErrorOr(
			trueErr1,
			func() error {
				panic("Short circuit before I blow up")
				return nil
			},
		),
		"Should short circuit true",
	)
}

func TestExistingErrorOrFT(t *testing.T) {
	assert.Equal(
		t,
		trueErr2,
		ExistingErrorOr(
			nil,
			func() error {
				return trueErr2
			},
		),
		"Function should be evaluated to an error",
	)
}

func TestExistingErrorOrFF(t *testing.T) {
	assert.Equal(
		t,
		nil,
		ExistingErrorOr(
			nil,
			func() error {
				return nil
			},
		),
		"Should evaluate to nil",
	)
}

func TestAnyErrorPositive(t *testing.T) {
	assert.Equal(
		t,
		trueErr1,
		AnyError(nil, trueErr1, nil),
		"Should return existing error",
	)
}

func TestAnyErrorNegative(t *testing.T) {
	assert.Equal(
		t,
		nil,
		AnyError(nil, nil, nil),
		"Should not return an error",
	)
}
