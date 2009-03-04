#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <ctype.h>
#include "bitvector.h"
#include "parse_options.h"
#include "hash_table.h"

const char *root = "";
bool lump_kernel;
bool lump_libraries;
Bitvector pid_include_vector(32768);
Bitvector pid_exclude_vector(32768);
bool include_some_pids;
bool exclude_some_pids;

HashTable<int> excluded_procedures(2000);
HashTable<int> included_procedures(2000);
bool exclude_some_procedures;
bool include_some_procedures;

bool exclude_kernel_syms;
bool exclude_library_syms;
bool include_kernel_syms;
bool include_library_syms;
bool demangle = true;

static const char *OptionsUsageStr =
    "  -e :kernel exclude all kernel symbols\n"
    "  -e :libs   exclude all library symbols\n"
    "  -e <func>  exclude function <func>\n"
    "  -e <pid>   exclude process <pid>\n"
    "  -i :kernel include all kernel symbols\n"
    "  -i :libs   include all library symbols\n"
    "  -i <func>  include function <func>\n"
    "  -i <pid>   include process <pid>\n"
    "  -l :kernel lump all the kernel symbols together\n"
    "  -l :libs   lump all the library symbols together\n"
    "  -m         do not demangle C++ symbols (m for 'mangle')\n"
    "  -r <root>  use <root> as the path for finding ELF executables\n"
    ;

void OptionsUsage()
{
    fprintf(stderr, "%s", OptionsUsageStr);
}

void ParseOptions(int argc, char **argv)
{
    bool err = false;
    while (!err) {
        int opt = getopt(argc, argv, "+e:i:l:mr:");
        if (opt == -1)
            break;
        switch (opt) {
        case 'e':
            if (*optarg == ':') {
                if (strcmp(optarg, ":kernel") == 0)
                    exclude_kernel_syms = true;
                else if (strcmp(optarg, ":libs") == 0)
                    exclude_library_syms = true;
                else
                    err = true;
                excluded_procedures.Update(optarg, 1);
                exclude_some_procedures = true;
            } else if (isdigit(*optarg)) {
                int bitnum = atoi(optarg);
                pid_exclude_vector.SetBit(bitnum);
                exclude_some_pids = true;
            } else {
                excluded_procedures.Update(optarg, 1);
                exclude_some_procedures = true;
            }
            break;
        case 'i':
            if (*optarg == ':') {
                if (strcmp(optarg, ":kernel") == 0)
                    include_kernel_syms = true;
                else if (strcmp(optarg, ":libs") == 0)
                    include_library_syms = true;
                else
                    err = true;
                included_procedures.Update(optarg, 1);
                include_some_procedures = true;
            } else if (isdigit(*optarg)) {
                int bitnum = atoi(optarg);
                pid_include_vector.SetBit(bitnum);
                include_some_pids = true;
            } else {
                included_procedures.Update(optarg, 1);
                include_some_procedures = true;
            }
            break;
        case 'l':
            if (strcmp(optarg, ":kernel") == 0)
                lump_kernel = true;
            else if (strcmp(optarg, ":libs") == 0)
                lump_libraries = true;
            else
                err = true;
            break;
        case 'm':
            demangle = false;
            break;
        case 'r':
            root = optarg;
            break;
        default:
            err = true;
            break;
        }
    }

    if (err) {
        Usage(argv[0]);
        exit(1);
    }
}
