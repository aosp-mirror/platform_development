#include <errno.h>
#include <fcntl.h>
#include <gelf.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#define PATH_MAX 256

static enum { HIDE_DUPS, PRUNE_DUPS, SHOW_DUPS } dup_mode;
static char *root_name;

static void app_err(const char *fmt, ...)
{
	va_list ap;

	va_start(ap, fmt);
	vfprintf(stderr, fmt, ap);
	va_end(ap);

	fprintf(stderr, "\n");
}

static void unix_err(const char *fmt, ...)
{
	va_list ap;
	int errsv;

	errsv = errno;

	va_start(ap, fmt);
	vfprintf(stderr, fmt, ap);
	va_end(ap);

	fprintf(stderr, ": %s\n", strerror(errsv));
}

static void elf_err(const char *fmt, ...)
{
	va_list ap;

	va_start(ap, fmt);
	vfprintf(stderr, fmt, ap);
	va_end(ap);

	fprintf(stderr, ": %s\n", elf_errmsg(-1));
}

struct seen {
	char *name;
	struct seen *next;
};

struct tree_state {
	int level;
	struct seen *seen;
};

static int seen(struct tree_state *t, char *name)
{
	struct seen *s;

	for (s = t->seen; s; s = s->next) {
		if (!strcmp(s->name, name))
			return 1;
	}

	return 0;
}

static void see(struct tree_state *t, char *name)
{
	struct seen *s = malloc(sizeof(*s));
	s->name = malloc(strlen(name) + 1);
	strcpy(s->name, name);
	s->next = t->seen;
	t->seen = s;
}

char *indent_str = "  ";

static void indent(struct tree_state *t)
{
	int i;

	for (i = 0; i < t->level; i++)
		printf("%s", indent_str);
}

struct search_dir {
	char *path;
	struct search_dir *next;
} *dirs = NULL;

static void add_search_dir(char *path)
{
	struct search_dir *dir = malloc(sizeof(*dir));
	dir->path = malloc(strlen(path) + 1);
	strcpy(dir->path, path);
	dir->next = dirs;
	dirs = dir;
}

struct file_state {
	struct tree_state *t;
	Elf *e;
	Elf_Data *strtab_data;
};

static Elf_Scn *find_scn(struct file_state *f, GElf_Word sht, Elf_Scn *scn, GElf_Shdr *shdr_out)
{
	while ((scn = elf_nextscn(f->e, scn))) {
		if (!gelf_getshdr(scn, shdr_out))
			continue;

		if (shdr_out->sh_type == sht)
			break;
	}

	return scn;
}

struct dyn_state {
	struct file_state *f;
	Elf_Data *dyn_data;
	int count;
};

static int find_dyn(struct dyn_state *d, GElf_Sxword tag, GElf_Dyn *dyn_out)
{
	int i;

	for (i = 0; i < d->count; i++) {
		if (!gelf_getdyn(d->dyn_data, i, dyn_out))
			continue;

		if (dyn_out->d_tag == tag)
			return 0;
	}

	return -1;
}

static int dump_file(struct tree_state *t, char *name, char *path);

static int dump_needed(struct tree_state *t, char *name)
{
	struct search_dir *dir;
	char path[PATH_MAX];
	int fd;

	t->level++;

	for (dir = dirs; dir; dir = dir->next) {
		snprintf(path, PATH_MAX, "%s/%s", dir->path, name);
		fd = open(path, O_RDONLY);
		if (fd >= 0) {
			close(fd);
			dump_file(t, name, path);
			t->level--;
			return 0;
		}
	}

	app_err("Couldn't resolve dependency \"%s\".", name);
	t->level--;
	return -1;
}

static int dump_dynamic(struct file_state *f, Elf_Scn *scn, GElf_Shdr *shdr)
{
	struct dyn_state d;
	GElf_Dyn needed_dyn;
	char *needed_name;
	int i;

	d.f = f;
	d.dyn_data = elf_getdata(scn, NULL);
	if (!d.dyn_data) {
		elf_err("elf_getdata failed");
		return -1;
	}
	d.count = shdr->sh_size / shdr->sh_entsize;

	for (i = 0; i < d.count; i++) {
		if (!gelf_getdyn(d.dyn_data, i, &needed_dyn))
			continue;

		if (needed_dyn.d_tag != DT_NEEDED)
			continue;

		needed_name = (char *)f->strtab_data->d_buf
			      + needed_dyn.d_un.d_val;

		dump_needed(f->t, needed_name);
	}

	return 0;
}

static int dump_file(struct tree_state *t, char *name, char *file)
{
	struct file_state f;
	int fd;
	Elf_Scn *scn;
	GElf_Shdr shdr;

	if ((dup_mode == HIDE_DUPS) && seen(t, name))
		return 0;

	indent(t); printf("%s", name);

	if ((dup_mode == PRUNE_DUPS) && seen(t, name)) {
		printf("...\n");
		return 0;
	} else {
		printf(":\n");
	}

	see(t, name);

	f.t = t;

	fd = open(file, O_RDONLY);
	if (fd < 0) {
		unix_err("open(%s) failed", file);
		return -1;
	}

	f.e = elf_begin(fd, ELF_C_READ, NULL);
	if (!f.e) {
		elf_err("elf_begin failed on %s", file);
		return -1;
	}

	scn = find_scn(&f, SHT_STRTAB, NULL, &shdr);
	f.strtab_data = elf_getdata(scn, NULL);
	if (!f.strtab_data) {
		app_err("%s has no strtab section", file);
		return -1;
	}

	scn = NULL;
	while ((scn = find_scn(&f, SHT_DYNAMIC, scn, &shdr))) {
		dump_dynamic(&f, scn, &shdr);
	}

	elf_end(f.e);
	close(fd);

	return 0;
}

static void usage(void)
{
	fprintf(stderr, "Usage: elftree [ -s | -h ] elf-file\n"
	                "  -S  Duplicate entire subtree when a duplicate is found\n"
			"  -P  Show duplicates, but only include subtree once\n"
			"  -H  Show each library at most once, even if duplicated\n"
			"  -h  Show this help screen\n");
}

static int parse_args(int argc, char *argv[])
{
	int i;

	for (i = 1; i < argc - 1; i++) {
		if (!strcmp(argv[i], "-S")) {
			dup_mode = SHOW_DUPS;
		} else if (!strcmp(argv[i], "-P")) {
			dup_mode = PRUNE_DUPS;
		} else if (!strcmp(argv[i], "-H")) {
			dup_mode = HIDE_DUPS;
		} else if (!strcmp(argv[i], "-h")) {
			usage();
			exit(0);
		} else {
			app_err("Unexpected argument \"%s\"!\n", argv[i]);
			return -1;
		}
	}

	root_name = argv[argc - 1];

	return 0;
}

static void add_search_dirs(void)
{
	char *relpath;
	char path[PATH_MAX];

	relpath = getenv("ANDROID_PRODUCT_OUT");
	if (!relpath) {
		app_err("Warning: ANDROID_PRODUCT_OUT not set; "
		        "using current directory.\n");
		relpath = ".";
	}

	snprintf(path, PATH_MAX, "%s/%s", relpath, "system/lib");
	add_search_dir(path);
}

int main(int argc, char *argv[])
{
	struct tree_state t;

	if (argc < 2 || parse_args(argc, argv)) {
		usage();
		exit(EXIT_FAILURE);
	}

	if (elf_version(EV_CURRENT) == EV_NONE) {
		elf_err("version mismatch");
		exit(EXIT_FAILURE);
	}

	t.level = 0;
	t.seen  = NULL;

	add_search_dirs();

	dump_file(&t, root_name, root_name);

	return 0;
}
