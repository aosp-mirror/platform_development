package interactors

import (
	"sort"
)

type simpleSet map[string]bool

// Returns the different of two slices of strings; effectively a set arithmetic operation on two slices of strings
func Difference(slice1, slice2 []string) []string {
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
