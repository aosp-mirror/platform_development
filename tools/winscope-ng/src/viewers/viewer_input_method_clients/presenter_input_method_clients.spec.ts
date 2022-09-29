/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { TraceType } from "common/trace/trace_type";
import { HierarchyTreeBuilder } from "test/unit/hierarchy_tree_builder";
import { PresenterInputMethodClients } from "./presenter_input_method_clients";
import { executePresenterInputMethodTests } from "viewers/common/presenter_input_method_test_utils";
import { UnitTestUtils } from "test/unit/utils";

describe("PresenterInputMethodClients", () => {
  async function getEntry() {
    return await UnitTestUtils.getInputMethodClientsEntry();
  }

  describe("PresenterInputMethod tests:", () => {
    const selectedTree = new HierarchyTreeBuilder().setName("8m23s29ms - InputMethodManager#showSoftInput")
      .setFilteredView(true).setKind("InputMethodClient entry").setId("entry").setStableId("entry")
      .setChildren([
        new HierarchyTreeBuilder().setKind("Client").setId("client").setFilteredView(true)
          .setStableId("client").build()
      ]).build();

    executePresenterInputMethodTests(
      getEntry,
      selectedTree,
      "elapsed",
      [3, 1],
      PresenterInputMethodClients,
      TraceType.INPUT_METHOD_CLIENTS,
    );
  });
});
