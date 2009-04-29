#include <debug.h>
#include <cmdline.h>
#include <stdio.h>
#include <stdlib.h>
#include <getopt.h>
#include <string.h>
#include <ctype.h>

extern char *optarg;
extern int optind, opterr, optopt;

static struct option long_options[] = {
    {"output",  required_argument, 0, 'o'},
    {"height",  required_argument, 0, 'h'},
    {"width",   required_argument, 0, 'w'},
    {"gray",    no_argument,       0, 'g'},
    {"type",    required_argument, 0, 't'},
    {"rotate",  required_argument, 0, 'r'},
    {"verbose", no_argument,       0, 'V'},
    {"help",    no_argument,       0, 1},
    {0, 0, 0, 0},
};

/* This array must parallel long_options[] */
static const char *descriptions[] = {
    "output file",
    "image height in pixels",
    "image width in pixels",
    "process the luma plane only",
    "encode as one of { 'ppm', 'rgb', or 'argb' }",
    "rotate (90, -90, 180 degrees)",
    "print verbose output",
    "print this help screen",
};

void print_help(const char *name) {
    fprintf(stdout,
            "Converts yuv 4:2:0 to rgb24 and generates a PPM file.\n"
            "invokation:\n"
            "\t%s infile --height <height> --width <width> --output <outfile> -t <ppm|grb|argb> [ --gray ] [ --rotate <degrees> ] [ --verbose ]\n"
            "\t%s infile --help\n",
            name, name);
    fprintf(stdout, "options:\n");
    struct option *opt = long_options;
    const char **desc = descriptions;
    while (opt->name) {
        fprintf(stdout, "\t-%c/--%s%s: %s\n",
                isprint(opt->val) ? opt->val : ' ',
                opt->name,
                (opt->has_arg ? " (argument)" : ""),
                *desc);
        opt++;
        desc++;
    }
}

int get_options(int argc, char **argv,
                char **outfile,
                int *height,
                int *width,
                int *gray,
                char **type,
                int *rotate,
                int *verbose) {
    int c;

    ASSERT(outfile); *outfile = NULL;
    ASSERT(height); *height = -1;
    ASSERT(width); *width = -1;
    ASSERT(gray); *gray = 0;
    ASSERT(rotate); *rotate = 0;
    ASSERT(verbose); *verbose = 0;
    ASSERT(type); *type = NULL;

    while (1) {
        /* getopt_long stores the option index here. */
        int option_index = 0;

        c = getopt_long (argc, argv,
                         "Vgo:h:w:r:t:",
                         long_options,
                         &option_index);
        /* Detect the end of the options. */
        if (c == -1) break;

        if (isgraph(c)) {
            INFO ("option -%c with value `%s'\n", c, (optarg ?: "(null)"));
        }

#define SET_STRING_OPTION(name) do {                                   \
    ASSERT(optarg);                                                    \
    (*name) = strdup(optarg);                                          \
} while(0)

#define SET_INT_OPTION(val) do {                                       \
    ASSERT(optarg);                                                    \
	if (strlen(optarg) >= 2 && optarg[0] == '0' && optarg[1] == 'x') { \
			FAILIF(1 != sscanf(optarg+2, "%x", val),                   \
				   "Expecting a hexadecimal argument!\n");             \
	} else {                                                           \
		FAILIF(1 != sscanf(optarg, "%d", val),                         \
			   "Expecting a decimal argument!\n");                     \
	}                                                                  \
} while(0)

        switch (c) {
        case 0:
            /* If this option set a flag, do nothing else now. */
            if (long_options[option_index].flag != 0)
                break;
            INFO ("option %s", long_options[option_index].name);
            if (optarg)
                INFO (" with arg %s", optarg);
            INFO ("\n");
            break;
        case 1: print_help(argv[0]); exit(1); break;
		case 'o':
			SET_STRING_OPTION(outfile);
			break;
		case 't':
			SET_STRING_OPTION(type);
			break;
        case 'h':
            SET_INT_OPTION(height);
            break;
        case 'w':
            SET_INT_OPTION(width);
            break;
        case 'r':
            SET_INT_OPTION(rotate);
            break;
        case 'g': *gray = 1; break;
        case 'V': *verbose = 1; break;
        case '?':
            /* getopt_long already printed an error message. */
            break;

#undef SET_STRING_OPTION
#undef SET_INT_OPTION

        default:
            FAILIF(1, "Unknown option");
        }
    }

    return optind;
}
