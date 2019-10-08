package interactors

import (
	cst "repodiff/constants"
	ent "repodiff/entities"
)

func ProjectNamesToType(manifests *ent.ManifestFileGroup) TypeMap {
	commonProjects := extractProjectNames(manifests.Common)
	distinctProjects := SetSubtract(
		SetUnion(
			extractProjectNames(manifests.Downstream),
			extractProjectNames(manifests.Upstream),
		),
		commonProjects,
	)
	return toMap(commonProjects, distinctProjects)
}

func extractProjectNames(m ent.ManifestFile) []string {
	projects := make([]string, len(m.Projects))
	for i, p := range m.Projects {
		projects[i] = p.Name
	}
	return projects
}

func toMap(common, distinct []string) TypeMap {
	ret := make(map[string]cst.ProjectType, len(common)+len(distinct))
	for _, k := range common {
		ret[k] = cst.Global
	}
	for _, k := range distinct {
		ret[k] = cst.DifferentialSpecific
	}
	return ret
}
