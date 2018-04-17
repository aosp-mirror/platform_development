package interactors

import (
	cst "repodiff/constants"
	ent "repodiff/entities"
)

func ApplyApplicationMutations(diffRows []ent.DiffRow, commitRows []ent.CommitRow, manifests *ent.ManifestFileGroup) ([]ent.AnalyzedDiffRow, []ent.AnalyzedCommitRow) {
	projectNameToType := ProjectNamesToType(manifests)
	return diffRowsToAnalyzed(diffRows, projectNameToType),
		commitRowsToAnalyzed(commitRows, projectNameToType)
}

func commitRowsToAnalyzed(commitRows []ent.CommitRow, projectNameToType TypeMap) []ent.AnalyzedCommitRow {
	analyzed := make([]ent.AnalyzedCommitRow, len(commitRows))
	for i, row := range commitRows {
		analyzed[i] = ent.AnalyzedCommitRow{
			CommitRow: row,
			Type: projectNameToType.getWithDefault(
				row.DownstreamProject,
				cst.Empty,
			),
		}
	}
	return analyzed
}

func diffRowsToAnalyzed(diffRows []ent.DiffRow, projectNameToType TypeMap) []ent.AnalyzedDiffRow {
	analyzed := make([]ent.AnalyzedDiffRow, len(diffRows))
	for i, row := range diffRows {
		analyzed[i] = ent.AnalyzedDiffRow{
			DiffRow: row,
			Type: projectNameToType.getWithDefault(
				row.DownstreamProject,
				cst.Empty,
			),
		}
	}
	return analyzed
}
