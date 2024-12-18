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

import {OriginAllowList} from './origin_allow_list';

describe('OriginAllowList', () => {
  describe('dev mode', () => {
    const mode = 'DEV' as const;

    it('allows localhost', () => {
      expect(
        OriginAllowList.isAllowed('http://localhost:8081', mode),
      ).toBeTrue();
      expect(
        OriginAllowList.isAllowed('https://localhost:8081', mode),
      ).toBeTrue();
    });
  });

  describe('prod mode', () => {
    const mode = 'PROD' as const;

    it('allows google.com', () => {
      expect(OriginAllowList.isAllowed('https://google.com', mode)).toBeTrue();
      expect(
        OriginAllowList.isAllowed('https://subdomain.google.com', mode),
      ).toBeTrue();
    });

    it('denies pseudo google.com', () => {
      expect(
        OriginAllowList.isAllowed('https://evilgoogle.com', mode),
      ).toBeFalse();
      expect(
        OriginAllowList.isAllowed('https://evil.com/google.com', mode),
      ).toBeFalse();
    });

    it('allows googleplex.com', () => {
      expect(
        OriginAllowList.isAllowed('https://googleplex.com', mode),
      ).toBeTrue();
      expect(
        OriginAllowList.isAllowed('https://subdomain.googleplex.com', mode),
      ).toBeTrue();
    });

    it('denies pseudo googleplex.com', () => {
      expect(
        OriginAllowList.isAllowed('https://evilgoogleplex.com', mode),
      ).toBeFalse();
      expect(
        OriginAllowList.isAllowed(
          'https://evil.com/subdomain.googleplex.com',
          mode,
        ),
      ).toBeFalse();
    });

    it('allows perfetto.dev', () => {
      expect(
        OriginAllowList.isAllowed('https://perfetto.dev', mode),
      ).toBeTrue();
      expect(
        OriginAllowList.isAllowed('https://subdomain.perfetto.dev', mode),
      ).toBeTrue();
    });

    it('denies pseudo perfetto.dev', () => {
      expect(
        OriginAllowList.isAllowed('https://evilperfetto.dev', mode),
      ).toBeFalse();
      expect(
        OriginAllowList.isAllowed(
          'https://evil.com/subdomain.perfetto.dev',
          mode,
        ),
      ).toBeFalse();
    });
  });
});
