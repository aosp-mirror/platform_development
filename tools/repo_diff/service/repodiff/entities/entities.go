package entities

type Project struct {
	URL    string `json:"url"`
	Branch string `json:"branch"`
}

type DiffTarget struct {
	Upstream   Project `json:"upstream"`
	Downstream Project `json:"downstream"`
}

type ApplicationConfig struct {
	OutputDirectory   string       `json:"output_directory"`
	AndroidProjectDir string       `json:"android_project_dir"`
	DiffScript        string       `json:"diff_script"`
	DiffTargets       []DiffTarget `json:"diff_targets"`
	Port              int          `json:"port"`
}

type DiffRow struct {
	Date                 string
	DownstreamProject    string
	UpstreamProject      string
	DiffStatus           int
	FilesChanged         int
	LineInsertions       int
	LineDeletions        int
	LineChanges          int
	CommitsNotUpstreamed int
	DBInsertTimestamp    int64
}

type CommitRow struct {
	Date              string
	Commit            string
	DownstreamProject string
	Author            string
	Subject           string
}

type MappedDiffTarget struct {
	UpstreamTarget   int16
	DownstreamTarget int16
}

type StatusMessage struct {
	JobStatus string
	Meta      string
}
