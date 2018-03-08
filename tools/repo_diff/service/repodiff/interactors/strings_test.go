package interactors

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestDifference(t *testing.T) {
	s1 := []string{
		"v1",
		"v2",
		"v3",
		"v4",
		// "v5",
	}
	s2 := []string{
		// "v1",
		"v2",
		// "v3",
		"v4",
		"v5",
	}
	expectedDiff := []string{
		"v1",
		"v3",
		"v5",
	}
	diff := Difference(s1, s2)
	assert.Equal(t, expectedDiff, diff, "Output differential of s1 and s2")
}

func TestDifferenceEmpty(t *testing.T) {
	var s1 []string
	var s2 []string

	diff := Difference(s1, s2)
	assert.Equal(t, 0, len(diff), "Output differential of s1 and s2")
}

func TestDifferenceDuplicates(t *testing.T) {
	s1 := []string{}
	s2 := []string{
		"v1",
		"v1",
		"v1",
	}
	expectedDiff := []string{
		"v1",
	}
	diff := Difference(s1, s2)
	assert.Equal(t, expectedDiff, diff, "Output differential of s1 and s2")
}
