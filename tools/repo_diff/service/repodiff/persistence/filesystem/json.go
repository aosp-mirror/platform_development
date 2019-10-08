package filesystem

import (
	"encoding/json"
	"io/ioutil"
)

const fourSpaces = "    "
const defaultReadPermissions = 0644

func WriteJsonSerializableToFile(jsonEntity interface{}, filename string) error {
	serialized, err := json.MarshalIndent(jsonEntity, "", fourSpaces)

	if err != nil {
		return err
	}

	return ioutil.WriteFile(
		filename,
		serialized,
		defaultReadPermissions,
	)
}

func ReadFileAsJson(filename string, outputEntityAddress interface{}) error {
	fileContents, err := ioutil.ReadFile(filename)

	if err != nil {
		return err
	}

	return json.Unmarshal(fileContents, outputEntityAddress)
}
