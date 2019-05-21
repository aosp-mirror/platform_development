// Copyright (C) 2019 The Android Open Source Project
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

#ifndef CONFIG_FILE_H_
#define CONFIG_FILE_H_

#include <cassert>
#include <iosfwd>
#include <map>
#include <string>


namespace header_checker {
namespace utils {


class ConfigParser;


class ConfigSection {
 public:
  using MapType = std::map<std::string, std::string>;
  using const_iterator = MapType::const_iterator;


 public:
  ConfigSection() = default;
  ConfigSection(ConfigSection &&) = default;
  ConfigSection &operator=(ConfigSection &&) = default;

  bool HasProperty(const std::string &name) const {
    return map_.find(name) != map_.end();
  }

  std::string GetProperty(const std::string &name) const {
    auto &&it = map_.find(name);
    if (it == map_.end()) {
      return "";
    }
    return it->second;
  }

  std::string operator[](const std::string &name) const {
    return GetProperty(name);
  }

  const_iterator begin() const {
    return map_.begin();
  }

  const_iterator end() const {
    return map_.end();
  }


 private:
  ConfigSection(const ConfigSection &) = delete;
  ConfigSection &operator=(const ConfigSection &) = delete;


 private:
  std::map<std::string, std::string> map_;

  friend class ConfigParser;
};


class ConfigFile {
 public:
  using MapType = std::map<std::string, ConfigSection>;
  using const_iterator = MapType::const_iterator;


 public:
  ConfigFile() = default;
  ConfigFile(ConfigFile &&) = default;
  ConfigFile &operator=(ConfigFile &&) = default;

  bool HasSection(const std::string &section_name) const {
    return map_.find(section_name) != map_.end();
  }

  const ConfigSection &GetSection(const std::string &section_name) const {
    auto &&it = map_.find(section_name);
    assert(it != map_.end());
    return it->second;
  }

  const ConfigSection &operator[](const std::string &section_name) const {
    return GetSection(section_name);
  }

  bool HasProperty(const std::string &section_name,
                   const std::string &property_name) const {
    auto &&it = map_.find(section_name);
    if (it == map_.end()) {
      return false;
    }
    return it->second.HasProperty(property_name);
  }

  std::string GetProperty(const std::string &section_name,
                          const std::string &property_name) const {
    auto &&it = map_.find(section_name);
    if (it == map_.end()) {
      return "";
    }
    return it->second.GetProperty(property_name);
  }

  const_iterator begin() const {
    return map_.begin();
  }

  const_iterator end() const {
    return map_.end();
  }


 private:
  ConfigFile(const ConfigFile &) = delete;
  ConfigFile &operator=(const ConfigFile &) = delete;


 private:
  std::map<std::string, ConfigSection> map_;

  friend class ConfigParser;
};


class ConfigParser {
 public:
  using ErrorListener = std::function<void (size_t, const char *)>;


 public:
  ConfigParser(std::istream &stream)
      : stream_(stream), section_(nullptr) { }

  ConfigFile ParseFile();

  static ConfigFile ParseFile(std::istream &istream);

  static ConfigFile ParseFile(const std::string &path);

  void SetErrorListener(ErrorListener listener) {
    error_listener_ = std::move(listener);
  }


 private:
  void ParseLine(size_t line_no, std::string_view line);

  void ReportError(size_t line_no, const char *cause) {
    if (error_listener_) {
      error_listener_(line_no, cause);
    }
  }


 private:
  ConfigParser(const ConfigParser &) = delete;
  ConfigParser &operator=(const ConfigParser &) = delete;


 private:
  std::istream &stream_;

  ErrorListener error_listener_;

  ConfigSection *section_;

  ConfigFile cfg_;
};


}  // namespace utils
}  // namespace header_checker


#endif  // CONFIG_FILE_H_
