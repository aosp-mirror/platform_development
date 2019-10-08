package interactors

import (
	cst "repodiff/constants"
)

type TypeMap map[string]cst.ProjectType

func (t TypeMap) getWithDefault(key string, fallback cst.ProjectType) cst.ProjectType {
	val, ok := t[key]
	if !ok {
		return fallback
	}
	return val
}
