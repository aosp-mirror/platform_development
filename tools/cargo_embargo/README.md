# cargo_embargo

`cargo_embargo` is a tool to generate `Android.bp` files automatically for external Rust crates
which are intended to build with cargo. It can be built with `m cargo_embargo` and run with
`cargo_embargo generate cargo_embargo.json` in a directory containing one or more Rust packages. If
successful this will write out an `Android.bp` file.

## Configuration

`cargo_embargo` is configured with a JSON configuration file usually called `cargo_embargo.json`.
This can contain a number of options, specified at several levels. A config may cover one or more
packages, and have one or more variants. All packages under `external/rust/crates` use a separate
`cargo_embargo.json` file per package, but other users (such as crosvm) may use a single
`cargo_embargo.json` for a tree of many packages. Most configurations have a single variant, but
multiple variants are used in some cases to provide both `std` and `no_std` variants of a package.

The overall structure of a config file looks like this:

```json
{
  // Top-level options, for all packages and variants.
  "package": {
    "package_name": {
      // Options for all variants of a package.
    },
    "another_package_name": {
      // ...
    }
  },
  "variants": [
    {
      // Options for a specific variant of all packages.
      "package": {
        "package_name": {
          // Options for a specific variant of a specific package.
        }
      }
    },
    {
      // Options for another variant.
    }
  ]
}
```

If a package is not included in the `package` map then it is assumed to use default options. If the
`package` map is omitted then all packages will use default options. If `variants` is omitted then
there is assumed to be a single variant. Thus `{}` is a valid config file for a single variant with
all default options.

A typical config file for a simple package may look like:

```json
{
  "run_cargo": false,
  "tests": true
}
```

This expands to a single variant with the given options, and all packages (i.e. the only one) using
default options.

### Top-level configuration options

These options may all be specified at the top level of the config file, or overridden per variant.

| Name                       | Type                      | Default                                                     | Meaning                                                                                                                                                                     |
| -------------------------- | ------------------------- | ----------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `tests`                    | boolean                   | `false`                                                     | Whether to output `rust_test` modules.                                                                                                                                      |
| `features`                 | list of strings           | -                                                           | Set of features to enable. If not set, uses the default crate features.                                                                                                     |
| `workspace`                | boolean                   | `false`                                                     | Whether to build with `--workspace`.                                                                                                                                        |
| `workspace_excludes`       | list of strings           | `[]`                                                        | When workspace is enabled, list of `--exclude` crates.                                                                                                                      |
| `global_defaults`          | string                    | -                                                           | Value to use for every generated module's `defaults` field.                                                                                                                 |
| `apex_available`           | list of strings           | `["//apex_available:platform", "//apex_available:anyapex"]` | Value to use for every generated library module's `apex_available` field.                                                                                                   |
| `native_bridge_supported`  | boolean                   | `false`                                                     | Value to use for every generated library module's `native_bridge_supported` field.                                                                                          |
| `product_available`        | boolean                   | `true`                                                      | Value to use for every generated library module's `product_available` field.                                                                                                |
| `ramdisk_available`        | boolean                   | `false`                                                     | Value to use for every generated library module's `ramdisk_available` field.                                                                                                |
| `recovery_available`       | boolean                   | `false`                                                     | Value to use for every generated library module's `recovery_available` field.                                                                                               |
| `vendor_available`         | boolean                   | `true`                                                      | Value to use for every generated library module's `vendor_available` field.                                                                                                 |
| `vendor_ramdisk_available` | boolean                   | `false`                                                     | Value to use for every generated library module's `vendor_ramdisk_available` field.                                                                                         |
| `min_sdk_version`          | string                    | -                                                           | Minimum SDK version for generated modules' `min_sdk_version` field.                                                                                                         |
| `module_name_overrides`    | string => string          | `{}`                                                        | Map of renames for modules. For example, if a "libfoo" would be generated and there is an entry ("libfoo", "libbar"), the generated module will be called "libbar" instead. |
| `cfg_blocklist`            | list of strings           | `[]`                                                        | `cfg` flags in this list will not be included.                                                                                                                              |
| `extra_cfg`                | list of strings           | `[]`                                                        | Extra `cfg` flags to enable in output modules.                                                                                                                              |
| `module_blocklist`         | list of strings           | `[]`                                                        | Modules in this list will not be generated.                                                                                                                                 |
| `module_visibility`        | string => list of strings | `{}`                                                        | Modules name => Soong "visibility" property.                                                                                                                                |
| `run_cargo`                | boolean                   | `true`                                                      | Whether to run the cargo build and parse its output, rather than just figuring things out from the cargo metadata.                                                          |
| `generate_androidbp`       | boolean                   | `true`                                                      | Whether to generate Android build rules in an `Android.bp` file.                                                                                                            |
| `generate_rulesmk`         | boolean                   | `false`                                                     | Whether to generate Trusty build rules in `rules.mk` and `Android.bp` files.                                                                                                |

Of particular note, it is preferable to set `run_cargo` to `false` where possible as it is
significantly faster. However, this may miss important details in more complicated cases, such as
packages with a `build.rs`, so it is recommended to run with `run_cargo` set to `true` initially,
and then compare the output when it is changed to `false`.

### Per-package configuration options

These options may be specified per package. Most may also be overridden per variant. They may not be
specified outside of a package.

| Name                    | Type                      | Default | Per variant | Meaning                                                                                                            |
| ----------------------- | ------------------------- | ------- | ----------- | ------------------------------------------------------------------------------------------------------------------ |
| `add_toplevel_block`    | path                      | -       | no          | File with content to append to the end of the generated Android.bp.                                                |
| `patch`                 | path                      | -       | no          | Patch file to apply after Android.bp is generated.                                                                 |
| `rulesmk_patch`         | path                      | -       | no          | Patch file to apply after rules.mk is generated.                                                                   |
| `license_text`          | list of paths             | -       | no          | Files to use for `license_text` in `license` module.                                                               |
| `alloc`                 | boolean                   | `false` | yes         | Link against `alloc`. Only valid if `no_std` is also true.                                                         |
| `device_supported`      | boolean                   | `true`  | yes         | Whether to compile for device. Defaults to true.                                                                   |
| `host_supported`        | boolean                   | `true`  | yes         | Whether to compile for host. Defaults to true.                                                                     |
| `host_first_multilib`   | boolean                   | `false` | yes         | Add a `compile_multilib: "first"` property to host modules.                                                        |
| `force_rlib`            | boolean                   | `false` | yes         | Generate "rust_library_rlib" instead of "rust_library".                                                            |
| `no_presubmit`          | boolean                   | `false` | yes         | Whether to disable "unit_test" for "rust_test" modules.                                                            |
| `add_module_block`      | path                      | -       | yes         | File with content to append to the end of each generated module.                                                   |
| `dep_blocklist`         | list of strings           | `[]`    | yes         | Modules in this list will not be added as dependencies of generated modules.                                       |
| `no_std`                | boolean                   | `false` | yes         | Don't link against `std`, only `core`.                                                                             |
| `copy_out`              | boolean                   | `false` | yes         | Copy `build.rs` output to `./out/*` and add a genrule to copy `./out/*` to genrule output.                         |
| `test_data`             | string => list of strings | `{}`    | yes         | Add the given files to the given tests' `data` property. The key is the test source filename relative to the crate |
| `whole_static_libs`     | list of strings           | `[]`    | yes         | Static libraries in this list will instead be added as whole_static_libs.                                          |
| `exported_c_header_dir` | list of paths             | `[]`    | yes         | Directories with headers to export for C usage.                                                                    |

## Auto-config

For importing a new package, you may start by running cargo_embargo's autoconfig mode:

```
cargo_embargo autoconfig cargo_embargo.json
```

This will attempt to generate a suitable `cargo_embargo.json` for the package in the current
directory, by trying with `run_cargo` both `true` and `false`, and including tests if there are any.
