#ifndef CMDLINE_H
#define CMDLINE_H

void print_help(const char *executable_name);

extern int get_options(int argc, char **argv,
                       char **outfile,
                       int *height,
                       int *width,
                       int *gray,
                       char **type,
                       int *rotate,
                       int *verbose);

#endif/*CMDLINE_H*/
