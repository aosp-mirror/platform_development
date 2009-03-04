#!/usr/bin/ruby
#
# Find unused resources in all the apps found recursively under the current directory
# Usage:
#   find_unused_resources.rb [-html]
#
# If -html is specified, the output will be HTML, otherwise it will be plain text
#
# Author: cbeust@google.com

require 'find'

debug = false

@@stringIdPattern = Regexp.new("name=\"([@_a-zA-Z0-9 ]*)\"")
@@layoutIdPattern = Regexp.new("android:id=\".*id/([_a-zA-Z0-9]*)\"")

@@stringXmlPatterns = [
  Regexp.new("@string/([_a-zA-Z0-9]*)"),
  Regexp.new("@array/([_a-zA-Z0-9]*)"),
]

@@javaIdPatterns = [
  Regexp.new("R.id.([_a-zA-Z0-9]+)"),
  Regexp.new("R.string.([_a-zA-Z0-9]+)"),
  Regexp.new("R.array.([_a-zA-Z0-9]+)"),
  Regexp.new("R.color.([_a-zA-Z0-9]+)"),
  Regexp.new("R.configVarying.([_a-zA-Z0-9]+)"),
  Regexp.new("R.dimen.([_a-zA-Z0-9]+)"),
]


@@appDir = "partner/google/apps/Gmail"

def findResDirectories(root)
  result = Array.new
  Find.find(root) do |path|
    if FileTest.directory?(path)
      if File.basename(path) == "res"
        result << path
      else
        next
      end
    end
  end
  result
end

class UnusedResources
  attr_accessor :appDir, :unusedLayoutIds, :unusedStringIds
end

class FilePosition
  attr_accessor :file, :lineNumber

  def initialize(f, ln)
    @file = f
    @lineNumber = ln
  end

  def to_s
    "#{file}:#{lineNumber}"
  end

  def <=>(other)
    if @file == other.file
      @lineNumber - other.lineNumber
    else
      @file <=> other.file
    end
  end
end


def findAllOccurrences(re, string)
  result = Array.new

  s = string
  matchData = re.match(s)
  while (matchData)
    result << matchData[1].to_s
    s = s[matchData.end(1) .. -1]
    matchData = re.match(s)
  end

  result
end

@@globalJavaIdUses = Hash.new

def recordJavaUses(glob)
  Dir.glob(glob).each { |filename|
    File.open(filename) { |file|
      file.each { |line|
	@@javaIdPatterns.each { |re|
          findAllOccurrences(re, line).each { |id|
            @@globalJavaIdUses[id] = FilePosition.new(filename, file.lineno)
	  }
        }
      }
    }
  }
end

def findUnusedResources(dir)
  javaIdUses = Hash.new
  layouts = Hash.new
  strings = Hash.new
  xmlIdUses = Hash.new

  Dir.glob("#{dir}/res/**/*.xml").each { |filename|
    if ! (filename =~ /attrs.xml$/)
      File.open(filename) { |file|
        file.each { |line|
          findAllOccurrences(@@stringIdPattern, line).each {|id|
            strings[id] = FilePosition.new(filename, file.lineno)
          }
          findAllOccurrences(@@layoutIdPattern, line).each {|id|
            layouts[id] = FilePosition.new(filename, file.lineno)
          }
          @@stringXmlPatterns.each { |re|
            findAllOccurrences(re, line).each {|id|
              xmlIdUses[id] = FilePosition.new(filename, file.lineno)
            }
          }
        }
      }
    end
  }
 
  Dir.glob("#{dir}/AndroidManifest.xml").each { |filename|
    File.open(filename) { |file|
      file.each { |line|
        @@stringXmlPatterns.each { |re|
          findAllOccurrences(re, line).each {|id|
            xmlIdUses[id] = FilePosition.new(filename, file.lineno)
          }
        }
      }
    }
  }

  recordJavaUses("#{dir}/src/**/*.java")

  @@globalJavaIdUses.each_pair { |id, file|
    layouts.delete(id)
    strings.delete(id)
  }

  javaIdUses.each_pair { |id, file|
    layouts.delete(id)
    strings.delete(id)
  }

  xmlIdUses.each_pair { |id, file|
    layouts.delete(id)
    strings.delete(id)
  }

  result = UnusedResources.new
  result.appDir = dir
  result.unusedLayoutIds = layouts
  result.unusedStringIds = strings

  result
end

def findApps(dir)
  result = Array.new
  Dir.glob("#{dir}/**/res").each { |filename|
    a = filename.split("/")
    result << a.slice(0, a.size-1).join("/")
  }
  result
end

def displayText(result)
  result.each { |unusedResources|
    puts "=== #{unusedResources.appDir}"

    puts "----- Unused layout ids"
    unusedResources.unusedLayoutIds.sort { |id, file| id[1] <=> file[1] }.each {|f|
      puts "    #{f[0]} #{f[1]}"
    }

 
    puts "----- Unused string ids"
    unusedResources.unusedStringIds.sort { |id, file| id[1] <=> file[1] }.each {|f|
      puts "    #{f[0]} #{f[1]}"
    }
 
  }
end

def displayHtmlUnused(unusedResourceIds, title)

  puts "<h3>#{title}</h3>"
  puts "<table border='1'>"
  unusedResourceIds.sort { |id, file| id[1] <=> file[1] }.each {|f|
    puts "<tr><td><b>#{f[0]}</b></td> <td>#{f[1]}</td></tr>"
  }
  puts "</table>"
end

def displayHtml(result)
  title = "Unused resources as of #{Time.now.localtime}"
  puts "<html><header><title>#{title}</title></header><body>"

  puts "<h1><p align=\"center\">#{title}</p></h1>"
  result.each { |unusedResources|
    puts "<h2>#{unusedResources.appDir}</h2>"
    displayHtmlUnused(unusedResources.unusedLayoutIds, "Unused layout ids")
    displayHtmlUnused(unusedResources.unusedStringIds, "Unused other ids")
  }
  puts "</body>"
end

result = Array.new

recordJavaUses("java/android/**/*.java")

if debug
  result << findUnusedResources("apps/Browser")
else 
  findApps(".").each { |appDir|
    result << findUnusedResources(appDir)
  }
end

if ARGV[0] == "-html"
  displayHtml result
else
  displayText result
end

