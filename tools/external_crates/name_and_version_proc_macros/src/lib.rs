// Copyright (C) 2023 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

use syn::{parse_macro_input, DeriveInput, Error};

#[proc_macro_derive(NameAndVersionMap)]
pub fn derive_name_and_version_map(input: proc_macro::TokenStream) -> proc_macro::TokenStream {
    let input = parse_macro_input!(input as DeriveInput);
    name_and_version_map::expand(input).unwrap_or_else(Error::into_compile_error).into()
}

mod name_and_version_map {
    use proc_macro2::TokenStream;
    use quote::quote;
    use syn::{
        Data, DataStruct, DeriveInput, Error, Field, GenericArgument, PathArguments, Result, Type,
    };

    pub(crate) fn expand(input: DeriveInput) -> Result<TokenStream> {
        let name = &input.ident;
        let (impl_generics, ty_generics, where_clause) = input.generics.split_for_impl();

        let mapfield = get_map_field(get_struct(&input)?)?;
        let mapfield_name = mapfield
            .ident
            .as_ref()
            .ok_or(Error::new_spanned(mapfield, "mapfield ident is none"))?;
        let (_, value_type) = get_map_type(&mapfield.ty)?;

        let expanded = quote! {
            #[automatically_derived]
            impl #impl_generics NameAndVersionMap for #name #ty_generics #where_clause {
                type Value = #value_type;

                fn map_field(&self) -> &BTreeMap<NameAndVersion, Self::Value> {
                    self.#mapfield_name.map_field()
                }

                fn map_field_mut(&mut self) -> &mut BTreeMap<NameAndVersion, Self::Value> {
                    self.#mapfield_name.map_field_mut()
                }

                fn insert_or_error(&mut self, key: NameAndVersion, val: Self::Value) -> Result<(), name_and_version::Error> {
                    self.#mapfield_name.insert_or_error(key, val)
                }

                fn num_crates(&self) -> usize {
                    self.#mapfield_name.num_crates()
                }

                fn get_versions<'a, 'b>(&'a self, name: &'b str) -> Box<dyn Iterator<Item = (&'a NameAndVersion, &'a Self::Value)> + 'a> {
                    self.#mapfield_name.get_versions(name)
                }

                fn get_versions_mut<'a, 'b>(&'a mut self, name: &'b str) -> Box<dyn Iterator<Item = (&'a NameAndVersion, &'a mut Self::Value)> + 'a> {
                    self.#mapfield_name.get_versions_mut(name)
                }

                fn filter_versions<'a: 'b, 'b, F: Fn(&mut dyn Iterator<Item = (&'b NameAndVersion, &'b Self::Value)>,
                ) -> HashSet<Version> + 'a>(
                    &'a self,
                    f: F,
                ) -> Box<dyn Iterator<Item =(&'a NameAndVersion, &'a Self::Value)> + 'a> {
                    self.#mapfield_name.filter_versions(f)
                }
            }
        };

        Ok(TokenStream::from(expanded))
    }

    fn get_struct(input: &DeriveInput) -> Result<&DataStruct> {
        match &input.data {
            Data::Struct(strukt) => Ok(strukt),
            _ => Err(Error::new_spanned(input, "Not a struct")),
        }
    }

    fn get_map_field(strukt: &DataStruct) -> Result<&Field> {
        for field in &strukt.fields {
            if let Ok((key_type, _value_type)) = get_map_type(&field.ty) {
                if let syn::Type::Path(path) = &key_type {
                    if path.path.segments.len() == 1
                        && path.path.segments[0].ident == "NameAndVersion"
                    {
                        return Ok(field);
                    }
                }
            }
        }
        return Err(Error::new_spanned(strukt.struct_token, "No field of type NameAndVersionMap"));
    }

    fn get_map_type(typ: &Type) -> Result<(&Type, &Type)> {
        if let syn::Type::Path(path) = &typ {
            if path.path.segments.len() == 1 && path.path.segments[0].ident == "BTreeMap" {
                if let PathArguments::AngleBracketed(args) = &path.path.segments[0].arguments {
                    if args.args.len() == 2 {
                        return Ok((get_type(&args.args[0])?, get_type(&args.args[1])?));
                    }
                }
            }
        }
        Err(Error::new_spanned(typ, "Must be BTreeMap"))
    }

    fn get_type(arg: &GenericArgument) -> Result<&Type> {
        if let GenericArgument::Type(typ) = arg {
            return Ok(typ);
        }
        Err(Error::new_spanned(arg, "Could not extract argument type"))
    }
}
