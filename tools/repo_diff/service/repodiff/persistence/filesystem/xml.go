package filesystem

import (
	"encoding/xml"
	"io/ioutil"
)

func ReadXMLAsEntity(filePath string, entity interface{}) error {
	xmlBytes, err := ioutil.ReadFile(filePath)
	if err != nil {
		return err
	}
	return xml.Unmarshal(xmlBytes, entity)
}
