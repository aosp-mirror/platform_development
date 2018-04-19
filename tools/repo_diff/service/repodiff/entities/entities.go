package entities

import (
	"repodiff/constants"
)

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
	CommonUpstream    Project      `json:"common_upstream"`
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

type AnalyzedDiffRow struct {
	DiffRow
	Type constants.ProjectType
}

type CommitRow struct {
	Date              string
	Commit            string
	DownstreamProject string
	Author            string
	Subject           string
}

type AnalyzedCommitRow struct {
	CommitRow
	Type constants.ProjectType
}

type MappedDiffTarget struct {
	UpstreamTarget   int16
	DownstreamTarget int16
}

type StatusMessage struct {
	JobStatus string
	Meta      string
}

type remote struct {
	Text   string `xml:",chardata"`
	Fetch  string `xml:"fetch,attr"`
	Name   string `xml:"name,attr"`
	Review string `xml:"review,attr"`
}

// "default" is the actual corresponding name in the XML tree but is also a reserved keyword in Golang; renamed as "defaultXML"
type defaultXML struct {
	Text       string `xml:",chardata"`
	DestBranch string `xml:"dest-branch,attr"`
	Remote     string `xml:"remote,attr"`
	Revision   string `xml:"revision,attr"`
	SyncJ      string `xml:"sync-j,attr"`
}

type manifestServer struct {
	Text string `xml:",chardata"`
	URL  string `xml:"url,attr"`
}

type copyFile struct {
	Text string `xml:",chardata"`
	Dest string `xml:"dest,attr"`
	Src  string `xml:"src,attr"`
}

type linkFile struct {
	Text string `xml:",chardata"`
	Dest string `xml:"dest,attr"`
	Src  string `xml:"src,attr"`
}

type ManifestProject struct {
	Text       string     `xml:",chardata"`
	Groups     string     `xml:"groups,attr"`
	Name       string     `xml:"name,attr"`
	CloneDepth string     `xml:"clone-depth,attr"`
	Path       string     `xml:"path,attr"`
	Copyfile   copyFile   `xml:"copyfile"`
	Linkfile   []linkFile `xml:"linkfile"`
}

type repoHooks struct {
	Text        string `xml:",chardata"`
	EnabledList string `xml:"enabled-list,attr"`
	InProject   string `xml:"in-project,attr"`
}

type ManifestFile struct {
	Text           string            `xml:",chardata"`
	Remote         remote            `xml:"remote"`
	Default        defaultXML        `xml:"default"`
	ManifestServer manifestServer    `xml:"manifest-server"`
	Projects       []ManifestProject `xml:"project"`
	RepoHooks      repoHooks         `xml:"repo-hooks"`
}

type ManifestFileGroup struct {
	Common     ManifestFile
	Upstream   ManifestFile
	Downstream ManifestFile
}

type RepoTimestamp int64
