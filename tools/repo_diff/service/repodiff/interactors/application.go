package interactors

import (
	cst "repodiff/constants"
	ent "repodiff/entities"
)

// AppProcessingParameters defines all possible inputs that are necessary
// prior to applying any application business logic. Any outputs should
// be derived from purely deterministic means; As such, the interactors
// package should be 100% testable and free of any error return types
type AppProcessingParameters struct {
	DiffRows   []ent.DiffRow
	CommitRows []ent.CommitRow
	Manifests  *ent.ManifestFileGroup
}

func ApplyApplicationMutations(p AppProcessingParameters) ([]ent.AnalyzedDiffRow, []ent.AnalyzedCommitRow) {
	projectNameToType := ProjectNamesToType(p.Manifests)
	return diffRowsToAnalyzed(p.DiffRows, projectNameToType),
		commitRowsToAnalyzed(p.CommitRows, projectNameToType)
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
