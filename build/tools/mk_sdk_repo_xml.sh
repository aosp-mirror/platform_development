#!/bin/bash
#
# Generates an SDK Repository XML based on the input files.

set -e

PROG_DIR=$(dirname $0)

TYPES="tool platform-tool build-tool platform sample doc add-on system-image source support"
OSES="linux macosx windows any linux-x86 darwin"

TMP_DIR=$(mktemp -d -t sdkrepo.tmp.XXXXXXXX)
trap "rm -rf $TMP_DIR" EXIT

function debug() {
  echo "DEBUG: " $@ > /dev/stderr
}

function error() {
  echo "*** ERROR: " $@
  usage
}

function usage() {
  cat <<EOFU
Usage: $0 output.xml xml-schema [type [os zip[:dest]]*...]*
where:
- schema is one of 'repository' or 'addon'
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

# Get the schema filename. E.g. ".../.../sdk-repository-10.xsd". Can be relative or absolute.
SCHEMA="$1"
[[ ! -f "$SCHEMA" ]] && error "Invalid XML schema name: $SCHEMA."
shift

# Get XML:NS for SDK from the schema
# This will be something like "http://schemas.android.com/sdk/android/addon/3"
XMLNS=$(sed -n '/xmlns:sdk="/s/.*"\(.*\)".*/\1/p' "$SCHEMA")
[[ -z "$XMLNS" ]] && error "Failed to find xmlns:sdk in $SCHEMA."
echo "## Using xmlns:sdk=$XMLNS"

# Extract the schema version number from the XMLNS, e.g. it would extract "3"
XSD_VERSION="${XMLNS##*/}"

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

# Definition of the attributes we read from source.properties or manifest.ini
# files and the equivalent XML element being generated.

ATTRS=(
  # Columns:
  # --------------------------+------------------------+----------------------
  # Name read from            | XML element written    | Min-XSD version
  # source.properties         | to repository.xml      | where XML can be used
  # --------------------------+------------------------+----------------------
  # from source.properties for repository.xml packages
  Pkg.Revision                  revision                 1
  Pkg.Desc                      description              1
  Platform.Version              version                  1
  AndroidVersion.ApiLevel       api-level                1
  AndroidVersion.CodeName       codename                 1
  Platform.IncludedAbi          included-abi             5
  Platform.MinToolsRev          min-tools-rev            1
  Platform.MinPlatformToolsRev  min-platform-tools-rev   3
  Sample.MinApiLevel            min-api-level            2
  Layoutlib.Api                 layoutlib/api            4
  Layoutlib.Revision            layoutlib/revision       4
  # from source.properties for addon.xml packages
  # (note that vendor is mapped to different XML elements based on the XSD version)
  Extra.VendorDisplay           vendor-display           4
  Extra.VendorId                vendor-id                4
  Extra.Vendor                  vendor-id                4
  Extra.Vendor                  vendor                   1
  Extra.NameDisplay             name-display             4
  Extra.Path                    path                     1
  Extra.OldPaths                old-paths                3
  Extra.MinApiLevel             min-api-level            2
  # for system-image
  SystemImage.Abi               abi                      r:3,s:1
  SystemImage.TagId             tag-id                   r:9,s:2
  SystemImage.TagDisplay        tag-display              r:9,s:2
  # from addon manifest.ini for addon.xml packages
  # (note that vendor/name are mapped to different XML elements based on the XSD version)
  vendor-id                     vendor-id                4
  vendor-display                vendor-display           4
  vendor                        vendor-display           4
  vendor                        vendor                   1
  name-id                       name-id                  4
  name-display                  name-display             4
  name                          name-display             4
  name                          name                     1
  description                   description              1
  api                           api-level                1
  version                       revision                 1
  revision                      revision                 1
)

# Start with repo-10, addon-7 and sys-img-3, we don't encode the os/arch
# in the <archive> attributes anymore. Instead we have separate elements.

function uses_new_host_os() {
  if [[ "$ROOT" == "sdk-repository" && "$XSD_VERSION" -ge "10" ]]; then return 0; fi
  if [[ "$ROOT" == "sdk-addon"      && "$XSD_VERSION" -ge  "7" ]]; then return 0; fi
  if [[ "$ROOT" == "sdk-sys-img"    && "$XSD_VERSION" -ge  "3" ]]; then return 0; fi
  return 1
}

ATTRS_ARCHIVE=(
  Archive.HostOs                host-os                   1
  Archive.HostBits              host-bits                 1
  Archive.JvmBits               jvm-bits                  1
  Archive.MinJvmVers            min-jvm-version           1
)


# Starting with XSD repo-7 and addon-5, some revision elements are no longer just
# integers. Instead they are in major.minor.micro.preview format. This defines
# which elements. This depends on the XSD root element and the XSD version.
#
# Note: addon extra revision can't take a preview number. We don't enforce
# this in this script. Instead schema validation will fail if the extra
# source.property declares an RC and it gets inserted in the addon.xml here.

if [[ "$ROOT" == "sdk-repository" && "$XSD_VERSION" -ge 7 ]] ||
   [[ "$ROOT" == "sdk-addon"      && "$XSD_VERSION" -ge 5 ]]; then
FULL_REVISIONS=(
  tool          revision
  build-tool    revision
  platform-tool revision
  extra         revision
  @             min-tools-rev
  @             min-platform-tools-rev
)
else
FULL_REVISIONS=()
fi


# Parse all archives.

function needs_full_revision() {
  local PARENT="$1"
  local ELEMENT="$2"
  shift
  shift
  local P E

  while [[ "$1" ]]; do
    P=$1
    E=$2
    if [[ "$E" == "$ELEMENT" ]] && [[ "$P" == "@" || "$P" == "$PARENT" ]]; then
      return 0 # true
    fi
    shift
    shift
  done

  return 1 # false
}

# Parses and print a full revision in the form "1.2.3 rc4".
# Note that the format requires to have 1 space before the
# optional "rc" (e.g. '1 rc4', not '1rc4') and no space after
# the rc (so not '1 rc 4' either)
function write_full_revision() {
  local VALUE="$1"
  local EXTRA_SPACE="$2"
  local KEYS="major minor micro preview"
  local V K

  while [[ -n "$VALUE" && -n "$KEYS" ]]; do
    # Take 1st segment delimited by . or space
    V="${VALUE%%[. ]*}"

    # Print it
    if [[ "${V:0:2}" == "rc" ]]; then
      V="${V:2}"
      K="preview"
      KEYS=""
    else
      K="${KEYS%% *}"
    fi

    if [[ -n "$V" && -n "$K" ]]; then
        echo "$EXTRA_SPACE            <sdk:$K>$V</sdk:$K>"
    fi

    # Take the rest.
    K="${KEYS#* }"
    if [[ "$K" == "$KEYS" ]]; then KEYS=""; else KEYS="$K"; fi
    V="${VALUE#*[. ]}"
    if [[ "$V" == "$VALUE" ]]; then VALUE=""; else VALUE="$V"; fi
  done
}


function parse_attributes() {
  local PROPS="$1"
  shift
  local RESULT=""
  local VALUE
  local REV
  local USED
  local S

  # Get the first letter of the schema name (e.g. sdk-repo => 'r')
  # This can be r, a or s and would match the min-XSD per-schema value
  # in the ATTRS list.
  S=$(basename "$SCHEMA")
  S="${S:4:1}"

  # $1 here is the ATTRS list above.
  while [[ "$1" ]]; do
    # Check the version in which the attribute was introduced and
    # ignore things which are too *new* for this schema. This lets
    # us generate old schemas for backward compatibility purposes.
    SRC=$1
    DST=$2
    REV=$3

    if [[ $REV =~ ([ras0-9:,]+,)?$S:([0-9])(,.*)? ]]; then
      # Per-schema type min-XSD revision. Format is "[<type>:rev],*]
      # where type is one of r, a or s matching $S above.
      REV="${BASH_REMATCH[2]}"
    fi

    if [[ $XSD_VERSION -ge $REV ]]; then
      # Parse the property, if present. Any space is replaced by @
      VALUE=$( grep "^$SRC=" "$PROPS" | cut -d = -f 2 | tr ' ' '@' | tr -d '\r' )
      if [[ -n "$VALUE" ]]; then
        # In case an XML element would be mapped multiple times,
        # only use its first definition.
        if [[ "${USED/$DST/}" == "$USED" ]]; then
          USED="$USED $DST"
          RESULT="$RESULT $DST $VALUE"
        fi
      fi
    fi
    shift
    shift
    shift
  done

  echo "$RESULT"
}

function output_attributes() {
  local ELEMENT="$1"
  local OUT="$2"
  shift
  shift
  local KEY VALUE
  local NODE LAST_NODE EXTRA_SPACE

  while [[ "$1" ]]; do
    KEY="$1"
    VALUE="${2//@/ }"
    NODE="${KEY%%/*}"
    KEY="${KEY##*/}"
    if [[ "$NODE" == "$KEY" ]]; then
      NODE=""
      EXTRA_SPACE=""
    fi
    if [[ "$NODE" != "$LAST_NODE" ]]; then
      EXTRA_SPACE="    "
      [[ "$LAST_NODE" ]] && echo "          </sdk:$LAST_NODE>" >> "$OUT"
      LAST_NODE="$NODE"
      [[ "$NODE"      ]] && echo "          <sdk:$NODE>" >> "$OUT"
    fi
    if needs_full_revision "$ELEMENT" "$KEY" ${FULL_REVISIONS[@]}; then
      echo "$EXTRA_SPACE        <sdk:$KEY>"       >> "$OUT"
      write_full_revision "$VALUE" "$EXTRA_SPACE" >> "$OUT"
      echo "$EXTRA_SPACE        </sdk:$KEY>"      >> "$OUT"
    else
      echo "$EXTRA_SPACE        <sdk:$KEY>$VALUE</sdk:$KEY>" >> "$OUT"
    fi
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
      output_attributes "$ELEMENT" "$OUT" $MAP
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

    if uses_new_host_os ; then
      USE_HOST_OS=1
    else
      OLD_OS_ATTR=" os='$OS'"
    fi

    cat >> "$OUT" <<EOFA
            <sdk:archive$OLD_OS_ATTR>
                <sdk:size>$SIZE</sdk:size>
                <sdk:checksum type='sha1'>$SHA1</sdk:checksum>
                <sdk:url>$DST</sdk:url>
EOFA
    if [[ $USE_HOST_OS ]]; then
      # parse the Archive.Host/Jvm info from the source.props if present
      MAP=$(parse_attributes "$PROPS" ${ATTRS_ARCHIVE[@]})
      # Always generate host-os if not present
      if [[ "${MAP/ host-os /}" == "$MAP" ]]; then
        MAP="$MAP host-os $OS"
      fi
      output_attributes "archive" "$OUT" $MAP
    fi
    echo "            </sdk:archive>" >> "$OUT"

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

