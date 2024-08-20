fn main() {
    protobuf_codegen::Codegen::new()
        // Pure Rust codegen. Not as well-tested, but avoids needing protoc installed.
        .pure()
        // All inputs and imports from the inputs must reside in `includes` directories.
        .include("src/protos")
        // Inputs must reside in some of include paths.
        .input("src/protos/metadata.proto")
        // Specify output directory relative to Cargo output directory.
        .cargo_out_dir("protos")
        .run_from_script();
}
