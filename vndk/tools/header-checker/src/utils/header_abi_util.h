// Copyright (C) 2017 The Android Open Source Project
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

#ifndef HEADER_ABI_UTIL_H_
#define HEADER_ABI_UTIL_H_

#include <map>
#include <regex>
#include <set>
#include <string>
#include <vector>


namespace header_checker {
namespace utils {


std::string RealPath(const std::string &path);

std::set<std::string> CollectAllExportedHeaders(
    const std::vector<std::string> &exported_header_dirs);

inline std::string FindAndReplace(const std::string &candidate_str,
                                  const std::string &find_str,
                                  const std::string &replace_str) {
  // Find all matches of find_str in candidate_str and return a new string with
  // all the matches replaced with replace_str
  std::regex match_expr(find_str);
  return std::regex_replace(candidate_str, match_expr, replace_str);
}

template <typename T, typename K>
std::vector<T> FindRemovedElements(
    const std::map<K, T> &old_elements_map,
    const std::map<K, T> &new_elements_map) {
  std::vector<T> removed_elements;
  for (auto &&map_element : old_elements_map) {
    auto element_key = map_element.first;
    auto new_element = new_elements_map.find(element_key);
    if (new_element == new_elements_map.end()) {
      removed_elements.emplace_back(map_element.second);
    }
  }
  return removed_elements;
}

template <typename K, typename T, typename Iterable, typename KeyGetter,
          typename ValueGetter>
inline void AddToMap(std::map<K, T> *dst, Iterable &src, KeyGetter get_key,
                     ValueGetter get_value) {
  for (auto &&element : src) {
    dst->insert(std::make_pair(get_key(&element), get_value(&element)));
  }
}

template <typename K, typename Iterable, typename KeyGetter>
inline void AddToSet(std::set<K> *dst, Iterable &src, KeyGetter get_key) {
  for (auto &&element : src) {
    dst->insert(get_key(element));
  }
}

template <typename K, typename T>
std::vector<std::pair<T, T>> FindCommonElements(
    const std::map<K, T> &old_elements_map,
    const std::map<K, T> &new_elements_map) {
  std::vector<std::pair<T, T>> common_elements;
  typename std::map<K, T>::const_iterator old_element =
      old_elements_map.begin();
  typename std::map<K, T>::const_iterator new_element =
      new_elements_map.begin();
  while (old_element != old_elements_map.end() &&
         new_element != new_elements_map.end()) {
    if (old_element->first == new_element->first) {
      common_elements.emplace_back(std::make_pair(
          old_element->second, new_element->second));
      old_element++;
      new_element++;
      continue;
    }
    if (old_element->first < new_element->first) {
      old_element++;
    } else {
      new_element++;
    }
  }
  return common_elements;
}


}  // namespace utils
}  // namespace header_checker


#endif  // HEADER_ABI_UTIL_H_
