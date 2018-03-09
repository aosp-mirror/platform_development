package repositories

import (
	e "repodiff/entities"
	"strings"
)

func cleanedDiffTarget(target e.DiffTarget) e.DiffTarget {
	return e.DiffTarget{
		Upstream: e.Project{
			URL:    protocolStrippedURL(target.Upstream.URL),
			Branch: target.Upstream.Branch,
		},
		Downstream: e.Project{
			URL:    protocolStrippedURL(target.Downstream.URL),
			Branch: target.Downstream.Branch,
		},
	}
}

func protocolStrippedURL(url string) string {
	startIndex := strings.Index(url, "//")
	return url[startIndex:]
}
