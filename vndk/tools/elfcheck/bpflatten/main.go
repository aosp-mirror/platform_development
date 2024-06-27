// Copyright (C) 2020 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"io"
	"os"
	"strings"

	"github.com/google/blueprint/parser"
)

type FlatModule struct {
	Type        string
	Name        string
	PropertyMap map[string]interface{}
}

func expandScalarTypeExpression(value parser.Expression) (scalar interface{}, isScalar bool) {
	if s, ok := value.(*parser.Bool); ok {
		return s.Value, true
	} else if s, ok := value.(*parser.String); ok {
		return s.Value, true
	} else if s, ok := value.(*parser.Int64); ok {
		return s.Value, true
	}
	return nil, false
}

func populatePropertyMap(propMap map[string]interface{}, prefix string, m *parser.Map) {
	for _, prop := range m.Properties {
		name := prop.Name
		if prefix != "" {
			name = prefix + "." + name
		}
		value := prop.Value
		if s, isScalar := expandScalarTypeExpression(value); isScalar {
			propMap[name] = s
		} else if list, ok := value.(*parser.List); ok {
			var l []interface{}
			for _, v := range list.Values {
				if s, isScalar := expandScalarTypeExpression(v); isScalar {
					l = append(l, s)
				}
			}
			propMap[name] = l
		} else if mm, ok := value.(*parser.Map); ok {
			populatePropertyMap(propMap, name, mm)
		}
	}
}

var anonymousModuleCount int

func flattenModule(module *parser.Module) (flattened FlatModule) {
	flattened.Type = module.Type
	if prop, found := module.GetProperty("name"); found {
		if value, ok := prop.Value.(*parser.String); ok {
			flattened.Name = value.Value
		}
	} else {
		flattened.Name = fmt.Sprintf("anonymous@<%d>", anonymousModuleCount)
		anonymousModuleCount++
	}
	flattened.PropertyMap = make(map[string]interface{})
	populatePropertyMap(flattened.PropertyMap, "", &module.Map)
	return flattened
}

func processFile(filename string, in io.Reader) ([]FlatModule, error) {
	if in == nil {
		if file, err := os.Open(filename); err != nil {
			return nil, err
		} else {
			defer file.Close()
			in = file
		}
	}

	ast, errs := parser.ParseAndEval(filename, in, &parser.Scope{})
	if len(errs) > 0 {
		for _, err := range errs {
			fmt.Fprintln(os.Stderr, err)
		}
		return nil, fmt.Errorf("%d parsing errors", len(errs))
	}

	var modules []FlatModule
	for _, def := range ast.Defs {
		if module, ok := def.(*parser.Module); ok {
			modules = append(modules, flattenModule(module))
		}
	}
	return modules, nil
}

func quoteBashString(s string) string {
	return strings.ReplaceAll(s, "$", "\\$")
}

func printBash(flatModules []FlatModule, w io.Writer) {
	var moduleNameList []string
	if len(flatModules) == 0 {
		// Early bail out if we have nothing to output
		return
	}
	fmt.Fprintf(w, "declare -a MODULE_NAMES\n")
	fmt.Fprintf(w, "declare -A MODULE_TYPE_DICT\n")
	fmt.Fprintf(w, "declare -A MODULE_PROP_KEYS_DICT\n")
	fmt.Fprintf(w, "declare -A MODULE_PROP_VALUES_DICT\n")
	fmt.Fprintf(w, "\n")
	for _, module := range flatModules {
		name := quoteBashString(module.Name)
		moduleNameList = append(moduleNameList, name)
		var modulePropKeys []string
		for k := range module.PropertyMap {
			modulePropKeys = append(modulePropKeys, k)
		}
		fmt.Fprintf(w, "MODULE_TYPE_DICT[%q]=%q\n", name, quoteBashString(module.Type))
		fmt.Fprintf(w, "MODULE_PROP_KEYS_DICT[%q]=%q\n", name,
			quoteBashString(strings.Join(modulePropKeys, " ")))
		for k, v := range module.PropertyMap {
			var propValue string
			if vl, ok := v.([]interface{}); ok {
				var list []string
				for _, s := range vl {
					list = append(list, fmt.Sprintf("%v", s))
				}
				propValue = fmt.Sprintf("%s", strings.Join(list, " "))
			} else {
				propValue = fmt.Sprintf("%v", v)
			}
			key := name + ":" + quoteBashString(k)
			fmt.Fprintf(w, "MODULE_PROP_VALUES_DICT[%q]=%q\n", key, quoteBashString(propValue))
		}
		fmt.Fprintf(w, "\n")
	}
	fmt.Fprintf(w, "MODULE_NAMES=(\n")
	for _, name := range moduleNameList {
		fmt.Fprintf(w, "  %q\n", name)
	}
	fmt.Fprintf(w, ")\n")
}

var (
	outputBashFlag = flag.Bool("bash", false, "Output in bash format")
	outputJsonFlag = flag.Bool("json", false, "Output in json format (this is the default)")
	helpFlag = flag.Bool("help", false, "Display this message and exit")
	exitCode = 0
)

func init() {
	flag.Usage = usage
}

func usage() {
	fmt.Fprintf(os.Stderr, "Usage: %s [OPTION]... [FILE]...\n", os.Args[0])
	fmt.Fprintf(os.Stderr, "Flatten Android.bp to python friendly json text.\n")
	fmt.Fprintf(os.Stderr, "If no file list is specified, read from standard input.\n")
	fmt.Fprintf(os.Stderr, "\n")
	flag.PrintDefaults()
}

func main() {
	defer func() {
		if err := recover(); err != nil {
			fmt.Fprintf(os.Stderr, "error: %v\n", err)
			exitCode = 1
		}
		os.Exit(exitCode)
	}()

	flag.Parse()

	if *helpFlag {
		usage()
		return
	}

	flatModules := []FlatModule{}

	if flag.NArg() == 0 {
		if modules, err := processFile("<stdin>", os.Stdin); err != nil {
			panic(err)
		} else {
			flatModules = append(flatModules, modules...)
		}
	}

	for _, pathname := range flag.Args() {
		switch fileInfo, err := os.Stat(pathname); {
		case err != nil:
			panic(err)
		case fileInfo.IsDir():
			panic(fmt.Errorf("%q is a directory", pathname))
		default:
			if modules, err := processFile(pathname, nil); err != nil {
				panic(err)
			} else {
				flatModules = append(flatModules, modules...)
			}
		}
	}

	if *outputBashFlag {
		printBash(flatModules, os.Stdout)
	} else {
		if b, err := json.MarshalIndent(flatModules, "", "  "); err != nil {
			panic(err)
		} else {
			fmt.Printf("%s\n", b)
		}
	}
}
