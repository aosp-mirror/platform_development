#!/usr/bin/perl
#
# Copyright (C) 2017 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

use Data::Dumper;

@strip_keys = (
    "Headers",
    "NameSpaces",
    "Sources",
    "TypeInfo",
    # in SymbolInfo
    "Class",
    "Header",
    "Line",
    "Param",
    "Return",
    "Source",
    "SourceLine"
);

sub StripDebug {
    my $arg = $_[0];
    if (ref($arg) ne "HASH") {
        return $arg;
    }
    my %out_hash = ();
    while ((my $key, my $value) = each %{$arg}) {
        if (not grep(/^$key$/, @strip_keys)) {
            $out_hash{$key} = StripDebug($value);
        }
    }
    return \%out_hash;
}


if ($#ARGV eq -1) {
    die "Usage: $0 DUMP_1 DUMP_2 ...\n";
}

$Data::Dumper::Sortkeys = 1;
for my $file_name (@ARGV) {
    require $file_name;
    $stripped = StripDebug($VAR1);

    open(FILE, ">", $file_name) or die "Cannot open $file_name: $!";
    print FILE Dumper($stripped);
    close FILE;
}

