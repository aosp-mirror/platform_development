#include <unistd.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <sys/stat.h>

#define BUFSIZE (1024*8)
static void to_unix(char* buf);
static void unix_to_dos(char* buf2, const char* buf);

int usage()
{
    fprintf(stderr, "usage: line_endings unix|dos FILES\n"
            "\n"
            "Convert FILES to either unix or dos line endings.\n");
    return 1;
}

typedef struct Node {
    struct Node *next;
    char buf[BUFSIZE*2+3];
} Node;

int
main(int argc, char** argv)
{
    enum { UNIX, DOS } ending;
    int i;

    if (argc < 2) {
        return usage();
    }

    if (0 == strcmp("unix", argv[1])) {
        ending = UNIX;
    }
    else if (0 == strcmp("dos", argv[1])) {
        ending = DOS;
    }
    else {
        return usage();
    }

    for (i=2; i<argc; i++) {
        int fd;
        int len;

        // force implied
        chmod(argv[i], S_IRUSR|S_IWUSR|S_IRGRP|S_IWGRP);

        fd = open(argv[i], O_RDWR);
        if (fd < 0) {
            fprintf(stderr, "unable to open file for read/write: %s\n", argv[i]);
            return 1;
        }

        len = lseek(fd, 0, SEEK_END);
        lseek(fd, 0, SEEK_SET);

        if (len > 0) {
            Node* root = malloc(sizeof(Node));
            Node* node = root;
            node->buf[0] = 0;

            while (len > 0) {
                node->next = malloc(sizeof(Node));
                node = node->next;
                node->next = NULL;

                char buf[BUFSIZE+2];
                ssize_t amt;
                ssize_t amt2 = len < BUFSIZE ? len : BUFSIZE;
                amt = read(fd, buf, amt2);
                if (amt != amt2) {
                    fprintf(stderr, "unable to read file: %s\n", argv[i]);
                    return 1;
                }
                buf[amt2] = '\0';
                to_unix(buf);
                if (ending == UNIX) {
                    strcpy(node->buf, buf);
                } else {
                    char buf2[(BUFSIZE*2)+3];
                    unix_to_dos(buf2, buf);
                    strcpy(node->buf, buf2);
                }
                len -= amt2;
            }

            ftruncate(fd, 0);
            lseek(fd, 0, SEEK_SET);
            while (root) {
                ssize_t amt2 = strlen(root->buf);
                if (amt2 > 0) {
                    ssize_t amt = write(fd, root->buf, amt2);
                    if (amt != amt2) {
                        fprintf(stderr, "unable to write file: %s\n", argv[i]);
                        return 1;
                    }
                }
                node = root;
                root = root->next;
                free(node);
            }
        }
        close(fd);
    }
    return 0;
}

void
to_unix(char* buf)
{
    char* p = buf;
    char* q = buf;
    while (*p) {
        if (p[0] == '\r' && p[1] == '\n') {
            // dos
            *q = '\n';
            p += 2;
            q += 1;
        }
        else if (p[0] == '\r') {
            // old mac
            *q = '\n';
            p += 1;
            q += 1;
        }
        else {
            *q = *p;
            p += 1;
            q += 1;
        }
    }
    *q = '\0';
}

void
unix_to_dos(char* buf2, const char* buf)
{
    const char* p = buf;
    char* q = buf2;
    while (*p) {
        if (*p == '\n') {
            q[0] = '\r';
            q[1] = '\n';
            q += 2;
            p += 1;
        } else {
            *q = *p;
            p += 1;
            q += 1;
        }
    }
    *q = '\0';
}

