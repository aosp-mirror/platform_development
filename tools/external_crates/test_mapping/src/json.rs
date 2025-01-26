// Copyright (C) 2024 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

//! Representation of a TEST_MAPPING JSON file.

use std::collections::BTreeSet;

use serde::{Deserialize, Serialize};

use crate::TestMappingError;

#[derive(Serialize, Deserialize, Default, Debug)]
pub(crate) struct TestMappingJson {
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub imports: Vec<TestMappingPath>,
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    presubmit: Vec<TestMappingName>,
    #[serde(default, skip_serializing_if = "Vec::is_empty", rename = "presubmit-rust")]
    presubmit_rust: Vec<TestMappingName>,
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    postsubmit: Vec<TestMappingName>,
}

#[derive(Serialize, Deserialize, Debug, Clone, PartialEq, Eq, PartialOrd, Ord)]
struct TestMappingName {
    name: String,
}

#[derive(Serialize, Deserialize, Debug, Clone, PartialEq, Eq, PartialOrd, Ord)]
pub(crate) struct TestMappingPath {
    pub path: String,
}

impl TestMappingJson {
    pub fn parse(mut contents: String) -> Result<TestMappingJson, TestMappingError> {
        let contents = contents.as_mut_str();
        // Comments are not part of the JSON spec (although they are often used), and Serde won't parse them.
        json_strip_comments::strip(contents).map_err(TestMappingError::StripJsonCommentsError)?;
        let parsed: TestMappingJson = serde_json::from_str(contents)?;
        Ok(parsed)
    }
    pub fn is_empty(&self) -> bool {
        self.imports.is_empty()
            && self.presubmit.is_empty()
            && self.presubmit_rust.is_empty()
            && self.postsubmit.is_empty()
    }
    pub fn remove_unknown_tests(&mut self, tests: &BTreeSet<String>) -> bool {
        let mut changed = false;
        if self.presubmit.iter().any(|t| !tests.contains(&t.name)) {
            self.presubmit.retain(|t| tests.contains(&t.name));
            changed = true;
        }
        if self.presubmit_rust.iter().any(|t| !tests.contains(&t.name)) {
            self.presubmit_rust.retain(|t| tests.contains(&t.name));
            changed = true;
        }
        if self.postsubmit.iter().any(|t| !tests.contains(&t.name)) {
            self.postsubmit.retain(|t| tests.contains(&t.name));
            changed = true;
        }
        changed
    }
    pub fn set_presubmits(&mut self, tests: &BTreeSet<String>) {
        self.presubmit = tests.iter().map(|t| TestMappingName { name: t.to_string() }).collect();
        self.presubmit_rust = self.presubmit.clone();
    }
    pub fn convert_postsubmit_tests(&mut self) -> bool {
        let changed = !self.postsubmit.is_empty();
        self.presubmit.append(&mut self.postsubmit.clone());
        self.presubmit_rust.append(&mut self.postsubmit);
        self.presubmit.sort();
        self.presubmit.dedup();
        self.presubmit_rust.sort();
        self.presubmit_rust.dedup();
        changed
    }
    pub fn add_new_tests_to_postsubmit(&mut self, tests: &BTreeSet<String>) -> bool {
        let mut changed = false;
        self.postsubmit.extend(tests.difference(&self.all_test_names()).map(|test_name| {
            changed = true;
            TestMappingName { name: test_name.to_string() }
        }));
        self.postsubmit.sort();
        self.postsubmit.dedup();
        changed
    }
    fn all_test_names(&self) -> BTreeSet<String> {
        let mut tests = BTreeSet::new();
        tests.extend(self.presubmit.iter().map(|t| t.name.clone()));
        tests.extend(self.presubmit_rust.iter().map(|t| t.name.clone()));
        tests.extend(self.postsubmit.iter().map(|t| t.name.clone()));
        tests
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    static TEST_JSON: &str = r###"{
  "imports": [
    {
      "path": "foo"
    }
  ],
  "presubmit": [
    {
      "name": "bar"
    }
  ],
  "presubmit-rust": [
    {
      "name": "baz"
    }
  ],
  "postsubmit": [
    {
      "name": "qux"
    }
  ]
}
"###;

    #[test]
    fn parse() -> Result<(), TestMappingError> {
        TestMappingJson::parse("{}".to_string())?;
        TestMappingJson::parse("//comment\n{}".to_string())?;
        TestMappingJson::parse(TEST_JSON.to_string())?;
        assert!(TestMappingJson::parse("foo".to_string()).is_err());
        Ok(())
    }

    #[test]
    fn all_test_names() -> Result<(), TestMappingError> {
        let json = TestMappingJson::parse(TEST_JSON.to_string())?;
        assert_eq!(
            json.all_test_names(),
            BTreeSet::from(["bar".to_string(), "baz".to_string(), "qux".to_string()])
        );
        Ok(())
    }

    #[test]
    fn set_presubmits() -> Result<(), TestMappingError> {
        let mut json = TestMappingJson::parse(TEST_JSON.to_string())?;
        json.set_presubmits(&BTreeSet::from(["asdf".to_string()]));
        assert_eq!(json.presubmit, vec![TestMappingName { name: "asdf".to_string() }]);
        assert_eq!(json.presubmit, json.presubmit_rust);
        Ok(())
    }

    #[test]
    fn add_new_tests_to_postsubmit() -> Result<(), TestMappingError> {
        let mut json = TestMappingJson::parse(TEST_JSON.to_string())?;
        assert!(!json.add_new_tests_to_postsubmit(&BTreeSet::from(["bar".to_string()])));
        assert!(json
            .add_new_tests_to_postsubmit(&BTreeSet::from(["bar".to_string(), "asdf".to_string()])));
        assert_eq!(
            json.postsubmit,
            vec![
                TestMappingName { name: "asdf".to_string() },
                TestMappingName { name: "qux".to_string() }
            ]
        );
        Ok(())
    }

    #[test]
    fn is_empty() -> Result<(), TestMappingError> {
        let json = TestMappingJson::parse(TEST_JSON.to_string())?;
        assert!(!json.is_empty());
        assert!(TestMappingJson::default().is_empty());
        Ok(())
    }
}
