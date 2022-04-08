// Copyright (C) 2017 The Android Open Source Project
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

#ifndef IR_REPRESENTATION_H_
#define IR_REPRESENTATION_H_

#include <list>
#include <map>
#include <memory>
#include <set>
#include <string>
#include <unordered_map>
#include <vector>


namespace header_checker {
namespace repr {


// Classes which act as middle-men between clang AST parsing routines and
// message format specific dumpers.

template <typename T>
using AbiElementMap = std::map<std::string, T>;

template <typename T>
using AbiElementUnorderedMap = std::unordered_map<std::string, T>;

template <typename T>
using AbiElementList = std::list<T>;

enum TextFormatIR {
  ProtobufTextFormat = 0,
  Json = 1,
};

enum CompatibilityStatusIR {
  Compatible = 0,
  UnreferencedChanges = 1,
  Extension = 4,
  Incompatible = 8,
  ElfIncompatible = 16
};

static inline CompatibilityStatusIR operator|(CompatibilityStatusIR f,
                                              CompatibilityStatusIR s) {
  return static_cast<CompatibilityStatusIR>(
      static_cast<std::underlying_type<CompatibilityStatusIR>::type>(f) |
      static_cast<std::underlying_type<CompatibilityStatusIR>::type>(s));
}

static inline CompatibilityStatusIR operator&(CompatibilityStatusIR f,
                                              CompatibilityStatusIR s) {
  return static_cast<CompatibilityStatusIR>(
      static_cast<std::underlying_type<CompatibilityStatusIR>::type>(f) &
      static_cast<std::underlying_type<CompatibilityStatusIR>::type>(s));
}

enum AccessSpecifierIR {
  PublicAccess = 1,
  ProtectedAccess = 2,
  PrivateAccess = 3
};

enum LinkableMessageKind {
  RecordTypeKind,
  EnumTypeKind,
  PointerTypeKind,
  QualifiedTypeKind,
  ArrayTypeKind,
  LvalueReferenceTypeKind,
  RvalueReferenceTypeKind,
  BuiltinTypeKind,
  FunctionTypeKind,
  FunctionKind,
  GlobalVarKind
};

template <typename K, typename V>
std::map<V, K> CreateInverseMap(const std::map<K, V> &m) {
  std::map<V, K> inverse_map;
  for (auto it : m) {
    inverse_map[it.second] = it.first;
  }
  return inverse_map;
}

class LinkableMessageIR {
 public:
  virtual ~LinkableMessageIR() {}

  const std::string &GetLinkerSetKey() const {
    return linker_set_key_;
  }

  void SetSourceFile(const std::string &source_file) {
    source_file_ = source_file;
  }

  void SetLinkerSetKey(const std::string &linker_set_key) {
    linker_set_key_ = linker_set_key;
  }

  const std::string &GetSourceFile() const {
    return source_file_;
  }

  virtual LinkableMessageKind GetKind() const = 0;

 protected:
  // The source file where this message comes from. This will be an empty string
  // for built-in types.
  std::string source_file_;
  std::string linker_set_key_;
};

class ReferencesOtherType {
 public:
  ReferencesOtherType(const std::string &referenced_type)
      : referenced_type_(referenced_type) {}

  ReferencesOtherType(std::string &&referenced_type)
      : referenced_type_(std::move(referenced_type)) {}

  ReferencesOtherType() {}

  void SetReferencedType(const std::string &referenced_type) {
    referenced_type_ = referenced_type;
  }

  const std::string &GetReferencedType() const {
    return referenced_type_;
  }

 protected:
  std::string referenced_type_;
};

// TODO: Break this up into types with sizes and those without types?
class TypeIR : public LinkableMessageIR, public ReferencesOtherType {
 public:
  virtual ~TypeIR() {}

  void SetSelfType(const std::string &self_type) {
    self_type_ = self_type;
  }

  const std::string &GetSelfType() const {
    return self_type_;
  }

  void SetName(const std::string &name) {
    name_ = name;
  }

  const std::string &GetName() const {
    return name_;
  }

  void SetSize(uint64_t size) {
    size_ = size;
  }

  uint64_t GetSize() const {
    return size_;
  }

  void SetAlignment(uint32_t alignment) {
    alignment_ = alignment;
  }

  uint32_t GetAlignment() const {
    return alignment_;
  }

 protected:
  std::string name_;
  std::string self_type_;
  uint64_t size_ = 0;
  uint32_t alignment_ = 0;
};

class VTableComponentIR {
 public:
  enum Kind {
    VCallOffset = 0,
    VBaseOffset = 1,
    OffsetToTop = 2,
    RTTI = 3,
    FunctionPointer = 4,
    CompleteDtorPointer = 5,
    DeletingDtorPointer = 6,
    UnusedFunctionPointer = 7
  };

  VTableComponentIR(const std::string &name, Kind kind, int64_t value,
                    bool is_pure)
      : component_name_(name), kind_(kind), value_(value), is_pure_(is_pure) {}

  VTableComponentIR() {}

  Kind GetKind() const {
    return kind_;
  }

  int64_t GetValue() const {
    return value_;
  }

  const std::string &GetName() const {
    return component_name_;
  }

  bool GetIsPure() const {
    return is_pure_;
  }

 protected:
  std::string component_name_;
  Kind kind_;
  int64_t value_ = 0;
  bool is_pure_;
};

class VTableLayoutIR {
 public:
  void AddVTableComponent(VTableComponentIR &&vtable_component) {
    vtable_components_.emplace_back(std::move(vtable_component));
  }

  const std::vector<VTableComponentIR> &GetVTableComponents() const {
    return vtable_components_;
  }

  uint64_t GetVTableNumEntries() const {
    return vtable_components_.size();
  }

 protected:
  std::vector<VTableComponentIR> vtable_components_;
};

class CXXBaseSpecifierIR : public ReferencesOtherType {
 public:
  CXXBaseSpecifierIR(const std::string &type, bool is_virtual,
                     AccessSpecifierIR access)
      : ReferencesOtherType(type), is_virtual_(is_virtual), access_(access) {}

  CXXBaseSpecifierIR() {}

  bool IsVirtual() const {
    return is_virtual_;
  }

  AccessSpecifierIR GetAccess() const {
    return access_;
  }

 protected:
  bool is_virtual_ = false;
  AccessSpecifierIR access_ = AccessSpecifierIR::PublicAccess;
};

class TemplateElementIR : public ReferencesOtherType {
 public:
  TemplateElementIR(std::string &&type)
      : ReferencesOtherType(std::move(type)) {}

  TemplateElementIR(const std::string &type)
      : ReferencesOtherType(type) {}

  TemplateElementIR() {}
};

class TemplateInfoIR {
 public:
  void AddTemplateElement(TemplateElementIR &&element) {
    template_elements_.emplace_back(element);
  }

  const std::vector<TemplateElementIR> &GetTemplateElements() const {
    return template_elements_;
  }

  std::vector<TemplateElementIR> &GetTemplateElements() {
    return template_elements_;
  }

 protected:
  std::vector<TemplateElementIR> template_elements_;
};

class TemplatedArtifactIR {
 public:
  void SetTemplateInfo(TemplateInfoIR &&template_info) {
    template_info_ = std::move(template_info);
  }

  const std::vector<TemplateElementIR> &GetTemplateElements() const {
    return template_info_.GetTemplateElements();
  }

  std::vector<TemplateElementIR> &GetTemplateElements() {
    return template_info_.GetTemplateElements();
  }

 protected:
  TemplateInfoIR template_info_;
};

class RecordFieldIR : public ReferencesOtherType {
 public:
  RecordFieldIR(const std::string &name, const std::string &type,
                uint64_t offset, AccessSpecifierIR access)
      : ReferencesOtherType(type), name_(name), offset_(offset),
        access_(access) {}

  RecordFieldIR() {}

  const std::string &GetName() const {
    return name_;
  }

  uint64_t GetOffset() const {
    return offset_;
  }

  AccessSpecifierIR GetAccess() const {
    return access_;
  }

 protected:
  std::string name_;
  uint64_t offset_ = 0;
  AccessSpecifierIR access_ = AccessSpecifierIR::PublicAccess;
};

class RecordTypeIR : public TypeIR, public TemplatedArtifactIR {
 public:
  enum RecordKind {
    struct_kind,
    class_kind,
    union_kind
  };

  void AddRecordField(RecordFieldIR &&field) {
    fields_.emplace_back(std::move(field));
  }

  void SetRecordFields(std::vector<RecordFieldIR> &&fields) {
    fields_ = std::move(fields);
  }

  void SetVTableLayout(VTableLayoutIR &&vtable_layout) {
    vtable_layout_ = std::move(vtable_layout);
  }

  const VTableLayoutIR &GetVTableLayout() const {
    return vtable_layout_;
  }

  void AddCXXBaseSpecifier(CXXBaseSpecifierIR &&base_specifier) {
    bases_.emplace_back(std::move(base_specifier));
  }

  void SetCXXBaseSpecifiers(std::vector<CXXBaseSpecifierIR> &&bases) {
    bases_ = std::move(bases);
  }

  const std::vector<CXXBaseSpecifierIR> &GetBases() const {
    return bases_;
  }

  std::vector<CXXBaseSpecifierIR> &GetBases() {
    return bases_;
  }

  void SetAccess(AccessSpecifierIR access) { access_ = access;}

  AccessSpecifierIR GetAccess() const {
    return access_;
  }

  const std::vector<RecordFieldIR> &GetFields() const {
    return fields_;
  }

  std::vector<RecordFieldIR> &GetFields() {
    return fields_;
  }

  LinkableMessageKind GetKind() const override {
    return LinkableMessageKind::RecordTypeKind;
  }

  uint64_t GetVTableNumEntries() const {
    return vtable_layout_.GetVTableNumEntries();
  }

  void SetRecordKind(RecordKind record_kind) {
    record_kind_ = record_kind;
  }

  RecordKind GetRecordKind() const {
    return record_kind_;
  }

  void SetAnonymity(bool is_anonymous) {
    is_anonymous_ = is_anonymous;
  }

  bool IsAnonymous() const {
    return is_anonymous_;
  }

 protected:
  std::vector<RecordFieldIR> fields_;
  VTableLayoutIR vtable_layout_;
  std::vector<CXXBaseSpecifierIR> bases_;
  AccessSpecifierIR access_ = AccessSpecifierIR::PublicAccess;
  bool is_anonymous_ = false;
  RecordKind record_kind_;
};

class EnumFieldIR {
 public:
  EnumFieldIR(const std::string &name, int value)
      : name_(name), value_(value) {}

  const std::string &GetName() const {
    return name_;
  }

  int GetValue() const {
    return value_;
  }

 protected:
  std::string name_;
  int value_ = 0;
};

class EnumTypeIR : public TypeIR {
 public:
  // Add Methods to get information from the IR.
  void AddEnumField(EnumFieldIR &&field) {
    fields_.emplace_back(std::move(field));
  }

  void SetAccess(AccessSpecifierIR access) { access_ = access;}

  LinkableMessageKind GetKind() const override {
    return LinkableMessageKind::EnumTypeKind;
  }

  AccessSpecifierIR GetAccess() const {
    return access_;
  }

  void SetUnderlyingType(std::string &&underlying_type) {
    underlying_type_ = std::move(underlying_type);
  }

  void SetUnderlyingType(const std::string &underlying_type) {
    underlying_type_ = underlying_type;
  }

  const std::string &GetUnderlyingType() const {
    return underlying_type_;
  }

  void SetFields(std::vector<EnumFieldIR> &&fields) {
    fields_ = std::move(fields);
  }

  const std::vector<EnumFieldIR> &GetFields() const {
    return fields_;
  }

 protected:
  std::vector<EnumFieldIR> fields_;
  std::string underlying_type_;
  AccessSpecifierIR access_ = AccessSpecifierIR::PublicAccess;
};

class ArrayTypeIR : public TypeIR {
 public:
  LinkableMessageKind GetKind() const override {
    return LinkableMessageKind::ArrayTypeKind;
  }
};

class PointerTypeIR : public TypeIR {
 public:
  LinkableMessageKind GetKind() const override {
    return LinkableMessageKind::PointerTypeKind;
  }
};

class BuiltinTypeIR : public TypeIR {
 public:
  void SetSignedness(bool is_unsigned) {
    is_unsigned_ = is_unsigned;
  }

  bool IsUnsigned() const {
    return is_unsigned_;
  }

  void SetIntegralType(bool is_integral_type) {
    is_integral_type_ = is_integral_type;
  }

  bool IsIntegralType() const {
    return is_integral_type_;
  }

 public:
  LinkableMessageKind GetKind() const override {
    return LinkableMessageKind::BuiltinTypeKind;
  }

 protected:
  bool is_unsigned_ = false;
  bool is_integral_type_ = false;
};

class LvalueReferenceTypeIR : public TypeIR {
 public:
  LinkableMessageKind GetKind() const override {
    return LinkableMessageKind::LvalueReferenceTypeKind;
  }
};

class RvalueReferenceTypeIR : public TypeIR {
 public:
  LinkableMessageKind GetKind() const override {
    return LinkableMessageKind::RvalueReferenceTypeKind;
  }
};

class QualifiedTypeIR : public TypeIR {
 public:
  void SetConstness(bool is_const) {
    is_const_ = is_const;
  }

  bool IsConst() const {
    return is_const_;
  }

  void SetRestrictedness(bool is_restricted) {
    is_restricted_ = is_restricted;
  }

  bool IsRestricted() const {
    return is_restricted_;
  }

  void SetVolatility(bool is_volatile) {
    is_volatile_ = is_volatile;
  }

  bool IsVolatile() const {
    return is_volatile_;
  }

 public:
  LinkableMessageKind GetKind() const override {
    return LinkableMessageKind::QualifiedTypeKind;
  }

 protected:
  bool is_const_;
  bool is_restricted_;
  bool is_volatile_;
};

class GlobalVarIR : public LinkableMessageIR , public ReferencesOtherType {
 public:
  // Add Methods to get information from the IR.
  void SetName(std::string &&name) {
    name_ = std::move(name);
  }

  void SetName(const std::string &name) {
    name_ = name;
  }

  const std::string &GetName() const {
    return name_;
  }

  void SetAccess(AccessSpecifierIR access) {
    access_ = access;
  }

  AccessSpecifierIR GetAccess() const {
    return access_;
  }

  LinkableMessageKind GetKind() const override {
    return LinkableMessageKind::GlobalVarKind;
  }

 protected:
  std::string name_;
  AccessSpecifierIR access_ = AccessSpecifierIR::PublicAccess;
};

class ParamIR : public ReferencesOtherType {
 public:
  ParamIR(const std::string &type, bool is_default, bool is_this_ptr)
      : ReferencesOtherType(type) , is_default_(is_default),
        is_this_ptr_(is_this_ptr) {}

  bool GetIsDefault() const {
    return is_default_;
  }

  bool GetIsThisPtr() const {
    return is_this_ptr_;
  }

 protected:
  bool is_default_ = false;
  bool is_this_ptr_ = false;
};

class CFunctionLikeIR {
 public:
  void SetReturnType(const std::string &type) {
    return_type_ = type;
  }

  const std::string &GetReturnType() const {
    return return_type_;
  }

  void AddParameter(ParamIR &&parameter) {
    parameters_.emplace_back(std::move(parameter));
  }

  const std::vector<ParamIR> &GetParameters() const {
    return parameters_;
  }

  std::vector<ParamIR> &GetParameters() {
    return parameters_;
  }

 protected:
  std::string return_type_;  // return type reference
  std::vector<ParamIR> parameters_;
};

class FunctionTypeIR : public TypeIR, public CFunctionLikeIR {
 public:
  LinkableMessageKind GetKind() const override {
    return LinkableMessageKind::FunctionTypeKind;
  }
};

class FunctionIR : public LinkableMessageIR, public TemplatedArtifactIR,
                   public CFunctionLikeIR {
 public:
  void SetAccess(AccessSpecifierIR access) {
    access_ = access;
  }

  AccessSpecifierIR GetAccess() const {
    return access_;
  }

  LinkableMessageKind GetKind() const override {
    return LinkableMessageKind::FunctionKind;
  }

  void SetName(const std::string &name) {
    name_ = name;
  }

  const std::string &GetName() const {
    return name_;
  }

 protected:
  std::string linkage_name_;
  std::string name_;
  AccessSpecifierIR access_ = AccessSpecifierIR::PublicAccess;
};

class ElfSymbolIR {
 public:
  enum ElfSymbolKind {
    ElfFunctionKind,
    ElfObjectKind,
  };

  enum ElfSymbolBinding {
    Weak,
    Global,
  };

  enum ElfSymbolVisibility {
    Default,
    Protected,
  };

 public:
  ElfSymbolIR(const std::string &name, ElfSymbolBinding binding)
      : name_(name), binding_(binding) {}

  virtual ~ElfSymbolIR() {}

  const std::string GetName() const {
    return name_;
  }

  ElfSymbolBinding GetBinding() const {
    return binding_;
  }

  virtual ElfSymbolKind GetKind() const = 0;

 protected:
  std::string name_;
  ElfSymbolBinding binding_;
};

class ElfFunctionIR : public ElfSymbolIR {
 public:
  ElfFunctionIR(const std::string &name, ElfSymbolBinding binding)
      : ElfSymbolIR(name, binding) {}

  ElfSymbolKind GetKind() const override {
    return ElfFunctionKind;
  }
};

class ElfObjectIR : public ElfSymbolIR {
 public:
  ElfObjectIR(const std::string &name, ElfSymbolBinding binding)
      : ElfSymbolIR(name, binding) {}

  ElfSymbolKind GetKind() const override {
    return ElfObjectKind;
  }
};

class ModuleIR {
 public:
  ModuleIR(const std::set<std::string> *exported_headers)
      : exported_headers_(exported_headers) {}

  const std::string &GetCompilationUnitPath() const {
    return compilation_unit_path_;
  }

  void SetCompilationUnitPath(const std::string &compilation_unit_path) {
    compilation_unit_path_ = compilation_unit_path;
  }

  const AbiElementMap<FunctionIR> &GetFunctions() const {
    return functions_;
  }

  const AbiElementMap<GlobalVarIR> &GetGlobalVariables() const {
    return global_variables_;
  }

  const AbiElementMap<RecordTypeIR> &GetRecordTypes() const {
    return record_types_;
  }

  const AbiElementMap<FunctionTypeIR> &GetFunctionTypes() const {
    return function_types_;
  }

  const AbiElementMap<EnumTypeIR> &GetEnumTypes() const {
    return enum_types_;
  }

  const AbiElementMap<LvalueReferenceTypeIR> &GetLvalueReferenceTypes() const {
    return lvalue_reference_types_;
  }

  const AbiElementMap<RvalueReferenceTypeIR> &GetRvalueReferenceTypes() const {
    return rvalue_reference_types_;
  }

  const AbiElementMap<QualifiedTypeIR> &GetQualifiedTypes() const {
    return qualified_types_;
  }

  const AbiElementMap<ArrayTypeIR> &GetArrayTypes() const {
    return array_types_;
  }

  const AbiElementMap<PointerTypeIR> &GetPointerTypes() const {
    return pointer_types_;
  }

  const AbiElementMap<BuiltinTypeIR> &GetBuiltinTypes() const {
    return builtin_types_;
  }

  const AbiElementMap<ElfFunctionIR> &GetElfFunctions() const {
    return elf_functions_;
  }

  const AbiElementMap<ElfObjectIR> &GetElfObjects() const {
    return elf_objects_;
  }

  const AbiElementMap<const TypeIR *> &GetTypeGraph() const {
    return type_graph_;
  }

  const AbiElementUnorderedMap<std::list<const TypeIR *>> &
  GetODRListMap() const {
    return odr_list_map_;
  }


  bool AddLinkableMessage(const LinkableMessageIR &);

  void AddFunction(FunctionIR &&function);

  void AddGlobalVariable(GlobalVarIR &&global_var);

  void AddRecordType(RecordTypeIR &&record_type);

  void AddFunctionType(FunctionTypeIR &&function_type);

  void AddEnumType(EnumTypeIR &&enum_type);

  void AddLvalueReferenceType(LvalueReferenceTypeIR &&lvalue_reference_type);

  void AddRvalueReferenceType(RvalueReferenceTypeIR &&rvalue_reference_type);

  void AddQualifiedType(QualifiedTypeIR &&qualified_type);

  void AddArrayType(ArrayTypeIR &&array_type);

  void AddPointerType(PointerTypeIR &&pointer_type);

  void AddBuiltinType(BuiltinTypeIR &&builtin_type);

  bool AddElfSymbol(const ElfSymbolIR &);

  void AddElfFunction(ElfFunctionIR &&elf_function);

  void AddElfObject(ElfObjectIR &&elf_object);

  void AddToODRListMap(const std::string &key, const TypeIR *value) {
    auto map_it = odr_list_map_.find(key);
    if (map_it == odr_list_map_.end()) {
      odr_list_map_.emplace(key, std::list<const TypeIR *>({value}));
      return;
    }
    odr_list_map_[key].emplace_back(value);
  }


 private:
  bool IsLinkableMessageInExportedHeaders(
      const LinkableMessageIR *linkable_message) const;


 public:
  // File path to the compilation unit (*.sdump)
  std::string compilation_unit_path_;

  AbiElementList<RecordTypeIR> record_types_list_;
  AbiElementMap<FunctionIR> functions_;
  AbiElementMap<GlobalVarIR> global_variables_;
  AbiElementMap<RecordTypeIR> record_types_;
  AbiElementMap<FunctionTypeIR> function_types_;
  AbiElementMap<EnumTypeIR> enum_types_;
  // These maps which contain generic referring types as values are used while
  // looking up whether in the parent graph, a particular referring type refers
  // to a certain type id. The mechanism is useful while trying to determine
  // whether a generic referring type needs to be newly added to the parent
  // graph or not.
  AbiElementMap<PointerTypeIR> pointer_types_;
  AbiElementMap<LvalueReferenceTypeIR> lvalue_reference_types_;
  AbiElementMap<RvalueReferenceTypeIR> rvalue_reference_types_;
  AbiElementMap<ArrayTypeIR> array_types_;
  AbiElementMap<BuiltinTypeIR> builtin_types_;
  AbiElementMap<QualifiedTypeIR> qualified_types_;
  AbiElementMap<ElfFunctionIR> elf_functions_;
  AbiElementMap<ElfObjectIR> elf_objects_;
  // type-id -> LinkableMessageIR * map
  AbiElementMap<const TypeIR *> type_graph_;
  // maps unique_id + source_file -> const TypeIR *
  AbiElementUnorderedMap<std::list<const TypeIR *>> odr_list_map_;
  const std::set<std::string> *exported_headers_;
};


}  // namespace repr
}  // namespace header_checker


#endif  // IR_REPRESENTATION_H_
