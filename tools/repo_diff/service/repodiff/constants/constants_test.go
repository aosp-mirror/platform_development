package constants

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestNullUUID(t *testing.T) {
	expected := "00000000-0000-0000-0000-000000000000"
	assert.Equal(t, expected, NullUUID().String(), "Null UUID should be deterministic")
	assert.Equal(t, NullUUID(), NullUUID(), "Equality verification")
}
