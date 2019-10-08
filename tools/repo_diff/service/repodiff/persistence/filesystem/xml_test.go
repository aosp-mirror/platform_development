package filesystem

import (
	"testing"

	"github.com/stretchr/testify/assert"

	e "repodiff/entities"
)

func TestReadXMLAsEntity(t *testing.T) {
	var manifest e.ManifestFile
	err := ReadXMLAsEntity("testdata/manifest.xml", &manifest)
	assert.Equal(t, nil, err, "Error should be nil")
	assert.Equal(t, 761, len(manifest.Projects), "Number of loaded projects")
}

func TestReadXMLAsEntityFileDoesNotExist(t *testing.T) {
	var manifest e.ManifestFile
	err := ReadXMLAsEntity("testdata/non_existent_file.xml", &manifest)
	assert.NotEqual(t, nil, err, "Error should be generated")
}
