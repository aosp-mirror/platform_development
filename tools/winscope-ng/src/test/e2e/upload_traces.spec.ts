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

import {browser, element, by} from "protractor";
import {E2eTestUtils} from "./utils";

describe("Upload traces", () => {
  beforeAll(async () => {
    await browser.manage().timeouts().implicitlyWait(5000);
  });

  beforeEach(async () => {
    browser.get("file://" + E2eTestUtils.getProductionIndexHtmlPath());
  });

  it("can process bugreport", async () => {
    await uploadBugreport();
    await checkHasLoadedTraces();
    await checkEmitsUnsupportedFileFormatMessages();
    await checkEmitsOverriddenTracesMessages();
    await clickLoadButton();
    await checkRendersSurfaceFlingerView();
  });

  const uploadBugreport = async () => {
    const inputFile = element(by.css("input[type=\"file\"]"));
    await inputFile.sendKeys(E2eTestUtils.getFixturePath("bugreports/bugreport_stripped.zip"));
  };

  const checkHasLoadedTraces = async () => {
    const text = await element(by.css(".uploaded-files")).getText();
    expect(text).toContain("wm_log.winscope (ProtoLog)");
    expect(text).toContain("ime_trace_service.winscope (IME Service)");
    expect(text).toContain("ime_trace_managerservice.winscope (IME Manager Service)");
    expect(text).toContain("wm_trace.winscope (Window Manager)");
    expect(text).toContain("layers_trace_from_transactions.winscope (Surface Flinger)");
    expect(text).toContain("ime_trace_clients.winscope (IME Clients)");
    expect(text).toContain("transactions_trace.winscope (Transactions)");
  };

  const checkEmitsUnsupportedFileFormatMessages = async () => {
    const text = await element(by.css("upload-snack-bar")).getText();
    expect(text).toContain("unsupported file format");
  };

  const checkEmitsOverriddenTracesMessages = async () => {
    const text = await element(by.css("upload-snack-bar")).getText();
    expect(text).toContain("overridden by another trace");
  };

  const clickLoadButton = async () => {
    const button = element(by.css(".load-btn"));
    await button.click();
  };

  const checkRendersSurfaceFlingerView = async () => {
    const viewerPresent = await element(by.css("viewer-surface-flinger")).isPresent();
    expect(viewerPresent).toBeTruthy();
  };
});
