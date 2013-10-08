#!/usr/bin/python
#
# This tool is used to compare headers between Bionic and NDK
# script should be in development/ndk/tools for correct roots autodetection
#

import sys, os, os.path
import subprocess
import argparse, textwrap

class FileCollector:
    """Collect headers from Bionic and sysroot

    sysincludes data format:
    sysincludes                     -- dict with arch as key
    sysincludes[arch]               -- dict with includes root as key
    sysincludes[arch][root]         -- dict with header name as key
    sysincludes[arch][root][header] -- list [last_platform, ..., first_platform]
    """

    def __init__(self, platforms_root, archs):
        """Init platform roots and structures before collecting"""
        self.platforms = []
        self.archs = archs
        self.sysincludes = {}
        for arch in self.archs:
            self.sysincludes[arch] = {}

        ## scaning available platforms ##
        for dirname in os.listdir(platforms_root):
            path = os.path.join(platforms_root, dirname)
            if os.path.isdir(path) and ('android' in dirname):
                self.platforms.append(dirname)
        try:
            self.platforms.sort(key = lambda s: int(s.split('-')[1]))
            self.root = platforms_root
        except Exception:
            print 'Wrong platforms list \n{0}'.format(str(self.platforms))

    def scan_dir(self, root):
        """Non-recursive file scan in directory"""
        files = []
        for filename in os.listdir(root):
            if os.path.isfile(os.path.join(root, filename)):
                files.append(filename)
        return files

    def scan_includes(self, root):
        """Recursive includes scan in given root"""
        includes = []
        includes_root = os.path.join(root, 'include')
        if not os.path.isdir(includes_root):
            return includes

        ## recursive scanning ##
        includes.append(('', self.scan_dir(includes_root)))
        for dirname, dirnames, filenames in os.walk(includes_root):
            for subdirname in dirnames:
                path = os.path.join(dirname, subdirname)
                relpath = os.path.relpath(path, includes_root)
                includes.append((relpath, self.scan_dir(path)))

        return includes

    def scan_archs_includes(self, root):
        """Scan includes for all defined archs in given root"""
        includes = {}
        includes['common'] = self.scan_includes(root)

        for arch in [a for a in self.archs if a != 'common']:
            arch_root = os.path.join(root, arch)
            includes[arch] = self.scan_includes(arch_root)

        return includes

    def scan_platform_includes(self, platform):
        """Scan all platform includes of one layer"""
        platform_root = os.path.join(self.root, platform)
        return self.scan_archs_includes(platform_root)

    def scan_bionic_includes(self, bionic_root):
        """Scan Bionic's libc includes"""
        self.bionic_root = bionic_root
        self.bionic_includes = self.scan_archs_includes(bionic_root)

    def append_sysincludes(self, arch, root, headers, platform):
        """Merge new platform includes layer with current sysincludes"""
        if not (root in self.sysincludes[arch]):
            self.sysincludes[arch][root] = {}

        for include in headers:
            if include in self.sysincludes[arch][root]:
                last_platform = self.sysincludes[arch][root][include][0]
                if platform != last_platform:
                    self.sysincludes[arch][root][include].insert(0, platform)
            else:
                self.sysincludes[arch][root][include] = [platform]

    def update_to_platform(self, platform):
        """Update sysincludes state by applying new platform layer"""
        new_includes = self.scan_platform_includes(platform)
        for arch in self.archs:
            for pack in new_includes[arch]:
                self.append_sysincludes(arch, pack[0], pack[1], platform)

    def scan_sysincludes(self, target_platform):
        """Fully automated sysincludes collector upto specified platform"""
        version = int(target_platform.split('-')[1])
        layers = filter(lambda s: int(s.split('-')[1]) <= version, self.platforms)
        for platform in layers:
            self.update_to_platform(platform)


class BionicSysincludes:
    def set_roots(self):
        """Automated roots initialization (AOSP oriented)"""
        script_root = os.path.dirname(os.path.realpath(__file__))
        self.aosp_root      = os.path.normpath(os.path.join(script_root, '../../..'))
        self.platforms_root = os.path.join(self.aosp_root, 'development/ndk/platforms')
        self.bionic_root    = os.path.join(self.aosp_root, 'bionic/libc')

    def scan_includes(self):
        """Scan all required includes"""
        self.collector = FileCollector(self.platforms_root, self.archs)
        ## detecting latest platform ##
        self.platforms = self.collector.platforms
        latest_platform = self.platforms[-1:][0]
        ## scanning both includes repositories ##
        self.collector.scan_sysincludes(latest_platform)
        self.collector.scan_bionic_includes(self.bionic_root)
        ## scan results ##
        self.sysincludes     = self.collector.sysincludes
        self.bionic_includes = self.collector.bionic_includes

    def git_diff(self, file_origin, file_probe):
        """Difference routine based on git diff"""
        try:
            subprocess.check_output(['git', 'diff', '--no-index', file_origin, file_probe])
        except subprocess.CalledProcessError as error:
            return error.output
        return None

    def match_with_bionic_includes(self):
        """Compare headers between Bionic and sysroot"""
        self.diffs = {}
        ## for every arch ##
        for arch in self.archs:
            arch_root = (lambda s: s if s != 'common' else '')(arch)
            ## for every includes directory ##
            for pack in self.bionic_includes[arch]:
                root = pack[0]
                path_bionic = os.path.join(self.bionic_root, arch_root, 'include', root)
                ## for every header that both in Bionic and sysroot ##
                for include in pack[1]:
                    if (root in self.sysincludes[arch]) and \
                    (include in self.sysincludes[arch][root]):
                        ## completing paths ##
                        platform = self.sysincludes[arch][root][include][0]
                        file_origin = os.path.join(path_bionic, include)
                        file_probe  = os.path.join(self.platforms_root, platform, arch_root, 'include', root, include)
                        ## comparison by git diff ##
                        output = self.git_diff(file_origin, file_probe)
                        if output is not None:
                            if arch not in self.diffs:
                                self.diffs[arch] = {}
                            if root not in self.diffs[arch]:
                                self.diffs[arch][root] = {}
                            ## storing git diff ##
                            self.diffs[arch][root][include] = output

    def print_history(self, arch, root, header):
        """Print human-readable list header updates across platforms"""
        history = self.sysincludes[arch][root][header]
        for platform in self.platforms:
            entry = (lambda s: s.split('-')[1] if s in history else '-')(platform)
            print '{0:3}'.format(entry),
        print ''

    def show_and_store_results(self):
        """Print summary list of headers and write diff-report to file"""
        try:
            diff_fd = open(self.diff_file, 'w')
            for arch in self.archs:
                if arch not in self.diffs:
                    continue
                print '{0}/'.format(arch)
                roots = self.diffs[arch].keys()
                roots.sort()
                for root in roots:
                    print '    {0}/'.format((lambda s: s if s != '' else '../include')(root))
                    includes = self.diffs[arch][root].keys()
                    includes.sort()
                    for include in includes:
                        print '        {0:32}'.format(include),
                        self.print_history(arch, root, include)
                        diff = self.diffs[arch][root][include]
                        diff_fd.write(diff)
                        diff_fd.write('\n\n')
                    print ''
                print ''

        finally:
            diff_fd.close()

    def main(self):
        self.set_roots()
        self.scan_includes()
        self.match_with_bionic_includes()
        self.show_and_store_results()

if __name__ == '__main__':
    ## configuring command line parser ##
    parser = argparse.ArgumentParser(formatter_class = argparse.RawTextHelpFormatter,
                                     description = 'Headers comparison tool between bionic and NDK platforms')
    parser.epilog = textwrap.dedent('''
    output format:
    {architecture}/
        {directory}/
            {header name}.h  {platforms history}

    platforms history format:
        number X means header has been changed in android-X
        `-\' means it is the same

    diff-report format:
        git diff output for all headers
        use --diff option to specify filename
    ''')

    parser.add_argument('--archs', metavar = 'A', nargs = '+',
                        default = ['common', 'arm', 'x86', 'mips'],
                        help = 'list of architectures\n(default: common arm x86 mips)')
    parser.add_argument('--diff', metavar = 'FILE', nargs = 1,
                        default = ['headers-diff-bionic-vs-ndk.diff'],
                        help = 'diff-report filename\n(default: `bionic-vs-sysincludes_report.diff\')')

    ## parsing arguments ##
    args = parser.parse_args()

    ## doing work ##
    app = BionicSysincludes()
    app.archs = map((lambda s: 'arch-{0}'.format(s) if s != 'common' else s), args.archs)
    app.diff_file = args.diff[0]
    app.main()

    print 'Headers listed above are DIFFERENT in Bionic and NDK platforms'
    print 'See `{0}\' for details'.format(app.diff_file)
    print 'See --help for format description.'
    print ''
