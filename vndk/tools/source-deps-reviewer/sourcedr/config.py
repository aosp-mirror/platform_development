#!/usr/bin/env python3

# The following file extensions are ignored
FILE_EXT_BLACK_LIST = {
    b'.1',
    b'.ac',
    b'.cmake',
    b'.html',
    b'.info',
    b'.la',
    b'.m4',
    b'.map',
    b'.md',
    b'.py',
    b'.rst',
    b'.sh',
    b'.sym',
    b'.txt',
    b'.xml',
}
# The following file names are ignored
FILE_NAME_BLACK_LIST = {
    b'CHANGES.0',
    b'ChangeLog',
    b'config.h.in',
    b'configure',
    b'configure.in',
    b'configure.linaro',
    b'libtool',
}
# If the following pattern occured in the file path,
# It will be ignored
PATH_PATTERN_BLACK_LIST = (
    b'autom4te.cache',
    b'dejagnu',
    b'llvm/Config/Config',
    b'/binutils/',
    b'.git',
    b'.repo',
    b'out',
)
