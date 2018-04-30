package interactors

import (
	"testing"

	"github.com/stretchr/testify/assert"

	cst "repodiff/constants"
	ent "repodiff/entities"
	"repodiff/persistence/filesystem"
)

func TestProjectNamesToType(t *testing.T) {
	var common, downstream, upstream ent.ManifestFile
	filesystem.ReadXMLAsEntity("testdata/common_manifest.xml", &common)
	filesystem.ReadXMLAsEntity("testdata/downstream_manifest.xml", &downstream)
	filesystem.ReadXMLAsEntity("testdata/upstream_manifest.xml", &upstream)

	nameToType := ProjectNamesToType(
		&ent.ManifestFileGroup{
			Common:     common,
			Upstream:   upstream,
			Downstream: downstream,
		},
	)
	assert.Equal(t, 777, len(nameToType), "expected")

	distinctCount := 0
	for _, projectType := range nameToType {
		if projectType == cst.DifferentialSpecific {
			distinctCount++
		}
	}
	assert.Equal(t, 153, distinctCount, "Expected count of distinct project names")
}
