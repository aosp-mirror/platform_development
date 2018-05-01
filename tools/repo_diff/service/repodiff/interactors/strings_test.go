package interactors

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestDistinctValues(t *testing.T) {
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
	diff := DistinctValues(s1, s2)
	assert.Equal(t, expectedDiff, diff, "Output differential of s1 and s2")
}

func TestDistinctValuesEmpty(t *testing.T) {
	var s1 []string
	var s2 []string

	diff := DistinctValues(s1, s2)
	assert.Equal(t, 0, len(diff), "Output differential of s1 and s2")
}

func TestDistinctValuesDuplicates(t *testing.T) {
	s1 := []string{}
	s2 := []string{
		"v1",
		"v1",
		"v1",
	}
	expectedDiff := []string{
		"v1",
	}
	diff := DistinctValues(s1, s2)
	assert.Equal(t, expectedDiff, diff, "Output differential of s1 and s2")
}

func TestSetSubtract(t *testing.T) {
	s1 := []string{
		"v1",
		"v2",
		"v3",
	}
	s2 := []string{
		"v2",
		"v3",
		"v4",
	}
	expected := []string{
		"v1",
	}
	diff := SetSubtract(s1, s2)
	assert.Equal(t, expected, diff, "Discard of s2 from s1")
}

func TestSetUnion(t *testing.T) {
	s1 := []string{
		"v1",
		"v2",
		"v3",
	}
	s2 := []string{
		"v2",
		"v3",
		"v4",
	}
	expected := []string{
		"v1",
		"v2",
		"v3",
		"v4",
	}
	union := SetUnion(s1, s2)
	assert.Equal(t, expected, union, "Union of s2 and s1")
}

func TestFilterNoUnicodeWithUnicode(t *testing.T) {
	regressionStr := "Move to AGP 3.0.0 stable 😁"
	assert.Equal(
		t,
		"Move to AGP 3.0.0 stable ",
		FilterNoUnicode(regressionStr),
		"Function should filter out unicode characters",
	)
}

func TestFilterNoUnicodeWithNoUnicode(t *testing.T) {
	validStr := "I'm a regular string with no whacky unicode chars"
	assert.Equal(
		t,
		validStr,
		FilterNoUnicode(validStr),
		"No change should occur",
	)
}
