package main

import (
	"flag"
)

var optionDiff = flag.Bool("execute-diff", true, "Specifies if a new (expensive) differential should be run")
var optionDenorm = flag.Bool("denormalize-data", true, "Specifies if existing historical data should be denormalized into viewable tables in DataStudio")
var optionReport = flag.Bool("generate-report", true, "Specifies if denormalized tables should be exported to the output directory as CSV's")

type enabledOperations struct {
	Diff   bool
	Denorm bool
	Report bool
}

func getEnabledOperations() enabledOperations {
	return enabledOperations{
		Diff:   *optionDiff,
		Denorm: *optionDenorm,
		Report: *optionReport,
	}
}

func init() {
	flag.Parse()
}
