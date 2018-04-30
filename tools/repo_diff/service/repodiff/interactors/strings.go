package interactors

import (
	"regexp"
	"sort"
	"strings"
)

type simpleSet map[string]bool

var unicode = regexp.MustCompile("[^\x00-\x7F]+")

func (s simpleSet) Contains(other string) bool {
	enabled, exists := s[other]
	return exists && enabled
}

// Returns the different of two slices of strings; effectively a set arithmetic operation on two slices of strings
func DistinctValues(slice1, slice2 []string) []string {
	sets := []simpleSet{
		sliceToSimpleSet(slice1),
		sliceToSimpleSet(slice2),
	}

	discardKeysFromOther(sets[0], sets[1])
	discardKeysFromOther(sets[1], sets[0])

	var exclusiveValues []string
	for _, k := range allKeys(sets...) {
		for _, set := range sets {
			if set[k] == true {
				exclusiveValues = append(exclusiveValues, k)
			}
		}
	}
	sort.Strings(exclusiveValues)
	return exclusiveValues
}

func SetSubtract(add, negate []string) []string {
	toRemove := sliceToSimpleSet(negate)
	var result []string
	for _, a := range add {
		if !toRemove.Contains(a) {
			result = append(result, a)
		}
	}
	return result
}

func SetUnion(slice1, slice2 []string) []string {
	union := allKeys(
		sliceToSimpleSet(
			append(
				slice1,
				slice2...,
			),
		),
	)
	sort.Strings(union)
	return union
}

func sliceToSimpleSet(s []string) simpleSet {
	m := make(simpleSet, len(s))
	for _, val := range s {
		m[val] = true
	}
	return m
}

func discardKeysFromOther(s1, s2 simpleSet) {
	for k := range s1 {
		if _, exists := s2[k]; exists {
			s2[k] = false
		}
	}
}

func allKeys(sets ...simpleSet) []string {
	totalCount := 0
	for _, s := range sets {
		totalCount += len(s)
	}

	keys := make([]string, totalCount)
	index := 0
	for _, s := range sets {
		for k := range s {
			keys[index] = k
			index++
		}
	}
	return keys
}

func FilterNoUnicode(s string) string {
	badCharacters := sliceToSimpleSet(
		unicode.FindAllString(s, -1),
	)
	if len(badCharacters) == 0 {
		return s
	}
	validCharacters := make([]string, 0, len(s))
	for _, rune_ := range s {
		char := string(rune_)
		if !badCharacters.Contains(char) {
			validCharacters = append(validCharacters, char)
		}
	}
	return strings.Join(
		validCharacters,
		"",
	)
}
