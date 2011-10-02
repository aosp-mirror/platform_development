#!/bin/bash
#
# Generates an SDK Repository XML based on the input files.

set -e

PROG_DIR=$(dirname $0)

TYPES="tool platform-tool platform sample doc add-on system-image source support"
OSES="linux macosx windows any linux-x86 darwin"

TMP_DIR=$(mktemp -d -t sdkrepo.tmp.XXXXXXXX)
trap "rm -rf $TMP_DIR" EXIT

function error() {
  echo "*** ERROR: " $@
  usage
}

function usage() {
  cat <<EOFU
Usage: $0 output.xml xml-schema [type [os zip[:dest]]*...]*
where:
- type is one of ${TYPES// /, } (or their plural).
- os   is one of  ${OSES// /, }.
There can be more than one zip for the same type
as long as they use different OSes.
Zip can be in the form "source:dest" to be renamed on the fly.
EOFU
  exit 1
}

# Validate the tools we need
if [[ ! -x $(which sha1sum) ]]; then
  error "Missing tool: sha1sum (Linux: apt-get install coreutils; Mac: port install md5sha1sum)"
fi

# Parse input params
OUT="$1"
[[ -z "$OUT" ]] && error "Missing output.xml name."
shift

SCHEMA="$1"
[[ ! -f "$SCHEMA" ]] && error "Invalid XML schema name: $SCHEMA."
shift

# Get XML:NS for SDK from the schema
XMLNS=$(sed -n '/xmlns:sdk="/s/.*"\(.*\)".*/\1/p' "$SCHEMA")
[[ -z "$XMLNS" ]] && error "Failed to find xmlns:sdk in $SCHEMA."
echo "## Using xmlns:sdk=$XMLNS"

# Get the root element from the schema. This is the first element
# which name starts with "sdk-" (e.g. sdk-repository, sdk-addon)
ROOT=$(sed -n -e '/xsd:element.*name="sdk-/s/.*name="\(sdk-[^"]*\)".*/\1/p' "$SCHEMA")
[[ -z "$ROOT" ]] && error "Failed to find root element in $SCHEMA."
echo "## Using root element $ROOT"

# Generate XML header
cat > "$OUT" <<EOFH
<?xml version="1.0"?>
<sdk:$ROOT
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:sdk="$XMLNS">
EOFH

# check_enum value value1 value2 value3...
# returns valueN if matched or nothing.
function check_enum() {
  local VALUE="$1"
  local i
  shift
  for i in "$@"; do
    if [[ "$i" == "$VALUE" ]]; then
      echo "$VALUE"
      break;
    fi
  done
}

# Parse all archives.

ATTRS=(
  # for repository packages
  Pkg.Revision                  revision
  Pkg.Desc                      description
  Platform.Version              version
  AndroidVersion.ApiLevel       api-level
  AndroidVersion.CodeName       codename
  Platform.IncludedAbi          included-abi
  Platform.MinToolsRev          min-tools-rev
  Platform.MinPlatformToolsRev  min-platform-tools-rev
  Extra.Vendor                  vendor
  Extra.Path                    path
  Extra.OldPaths                old-paths
  Extra.MinApiLevel             min-api-level
  Sample.MinApiLevel            min-api-level
  SystemImage.Abi               abi
  Layoutlib.Api                 layoutlib/api
  Layoutlib.Revision            layoutlib/revision
  # for addon packages
  vendor                        vendor
  name                          name
  description                   description
  api                           api-level
  version                       revision
  revision                      revision
)

function parse_attributes() {
  local PROPS="$1"
  shift
  local RESULT=""
  local VALUE

  while [[ "$1" ]]; do
    # Parse the property, if present. Any space is replaced by @
    VALUE=$( grep "^$1=" "$PROPS" | cut -d = -f 2 | tr ' ' '@' | tr -d '\r' )
    if [[ -n "$VALUE" ]]; then
      RESULT="$RESULT $2 $VALUE"
    fi
    shift
    shift
  done

  echo "$RESULT"
}

function output_attributes() {
  local OUT="$1"
  shift
  local KEY VALUE
  local NODE LAST_NODE

  while [[ "$1" ]]; do
    KEY="$1"
    VALUE="${2//@/ }"
    NODE="${KEY%%/*}"
    KEY="${KEY##*/}"
    [[ "$NODE" == "$KEY" ]] && NODE=""
    if [[ "$NODE" != "$LAST_NODE" ]]; then
        [[ "$LAST_NODE" ]] && echo "          </sdk:$LAST_NODE>" >> "$OUT"
        LAST_NODE="$NODE"
        [[ "$NODE"      ]] && echo "          <sdk:$NODE>" >> "$OUT"
    fi
    echo "        <sdk:$KEY>$VALUE</sdk:$KEY>" >> "$OUT"
    shift
    shift
  done
  if [[ "$LAST_NODE" ]]; then echo "          </sdk:$LAST_NODE>" >> "$OUT"; fi
}

while [[ -n "$1" ]]; do
  # Process archives.
  # First we expect a type. For convenience the type can be plural.
  TYPE=$(check_enum "${1%%s}" $TYPES)
  [[ -z $TYPE ]] && error "Unknown archive type '$1'."
  shift

  ELEMENT="$TYPE"
  # The element name is different for extras:
  [[ "$TYPE" == "support" ]] && ELEMENT="extra"

  MAP=""
  FIRST="1"
  LIBS_XML=""

  OS=$(check_enum "$1" $OSES)
  while [[ $OS ]]; do
    shift
    [[ $OS == "linux-x86" ]] && OS=linux
    [[ $OS == "darwin" ]] && OS=macosx

    SRC="$1"
    DST="$1"
    if [[ "${SRC/:/}" != "$SRC" ]]; then
      DST="${SRC/*:/}"
      SRC="${SRC/:*/}"
    fi
    [[ ! -f "$SRC" ]] && error "Missing file for archive $TYPE/$OS: $SRC"
    shift

    # Depending on the archive type, we need a number of attributes
    # from the source.properties or the manifest.ini. We'll take
    # these attributes from the first zip found.
    #
    # What we need vs. which package uses it:
    # - description             all
    # - revision                all
    # - version                 platform
    # - included-abi            platform
    # - api-level               platform sample doc add-on system-image
    # - codename                platform sample doc add-on system-image
    # - min-tools-rev           platform sample
    # - min-platform-tools-rev  tool
    # - min-api-level           extra
    # - vendor                  extra               add-on
    # - path                    extra
    # - old-paths               extra
    # - abi                     system-image
    #
    # We don't actually validate here.
    # Just take whatever is defined and put it in the XML.
    # XML validation against the schema will be done at the end.

    if [[ $FIRST ]]; then
      FIRST=""

      if unzip -t "$SRC" | grep -qs "source.properties" ; then
        # Extract Source Properties
        # unzip: -j=flat (no dirs), -q=quiet, -o=overwrite, -d=dest dir
        unzip -j -q -o -d "$TMP_DIR" "$SRC" "*/source.properties"
        PROPS="$TMP_DIR/source.properties"

      elif unzip -t "$SRC" | grep -qs "manifest.ini" ; then
        unzip -j -q -o -d "$TMP_DIR" "$SRC" "*/manifest.ini"
        PROPS="$TMP_DIR/manifest.ini"

        # Parse the libs for an addon and generate the <libs> node
        # libraries is a semi-colon separated list
        LIBS=$(parse_attributes "$PROPS" "libraries")
        LIBS_XML="        <sdk:libs>"
        for LIB in ${LIBS//;/ }; do
          LIBS_XML="$LIBS_XML
           <sdk:lib><sdk:name>$LIB</sdk:name></sdk:lib>"
        done
        LIBS_XML="$LIBS_XML
        </sdk:libs>"

      else
        error "Failed to find source.properties or manifest.ini in $SRC"
      fi

      [[ ! -f $PROPS ]] && error "Failed to extract $PROPS from $SRC"
      MAP=$(parse_attributes "$PROPS" ${ATTRS[@]})

      # Time to generate the XML for the package
      echo "    <sdk:${ELEMENT}>" >> "$OUT"
      output_attributes "$OUT" $MAP
      [[ -n "$LIBS_XML" ]] && echo "$LIBS_XML" >> "$OUT"
      echo "        <sdk:archives>" >> "$OUT"
    fi

    # Generate archive info
    echo "## Add $TYPE/$OS archive $SRC"
    if [[ $( uname ) == "Darwin" ]]; then
      SIZE=$( stat -f %z "$SRC" )
    else
      SIZE=$( stat -c %s "$SRC" )
    fi
    SHA1=$( sha1sum "$SRC" | cut -d " "  -f 1 )

    cat >> "$OUT" <<EOFA
            <sdk:archive os='$OS' arch='any'>
                <sdk:size>$SIZE</sdk:size>
                <sdk:checksum type='sha1'>$SHA1</sdk:checksum>
                <sdk:url>$DST</sdk:url>
            </sdk:archive>
EOFA

    # Skip to next arch/zip entry.
    # If not a valid OS, close the archives/package nodes.
    OS=$(check_enum "$1" $OSES)

    if [[ ! "$OS" ]]; then
      echo "        </sdk:archives>" >> "$OUT"
      echo "    </sdk:${ELEMENT}>" >> "$OUT"
    fi
  done

done

# Generate XML footer
echo "</sdk:$ROOT>" >> "$OUT"

echo "## Validate XML against schema"
xmllint --schema $SCHEMA "$OUT"
