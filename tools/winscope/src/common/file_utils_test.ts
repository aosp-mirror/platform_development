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
import {UnitTestUtils} from 'test/unit/utils';
import {FileUtils} from './file_utils';

describe('FileUtils', () => {
  it('extracts file extensions', () => {
    expect(FileUtils.getFileExtension('winscope.zip')).toEqual('zip');
    expect(FileUtils.getFileExtension('win.scope.zip')).toEqual('zip');
    expect(FileUtils.getFileExtension('winscopezip')).toEqual(undefined);
  });

  it('removes directory from filename', () => {
    expect(FileUtils.removeDirFromFileName('test/winscope.zip')).toEqual(
      'winscope.zip',
    );
    expect(FileUtils.removeDirFromFileName('test/test/winscope.zip')).toEqual(
      'winscope.zip',
    );
  });

  it('removes extension from filename', () => {
    expect(FileUtils.removeExtensionFromFilename('winscope.zip')).toEqual(
      'winscope',
    );
    expect(FileUtils.removeExtensionFromFilename('win.scope.zip')).toEqual(
      'win.scope',
    );
  });

  it('creates zip archive', async () => {
    const zip = await FileUtils.createZipArchive([
      new File([], 'test_file.txt'),
    ]);
    expect(zip).toBeInstanceOf(Blob);
  });

  it('creates zip archive with progress listener', async () => {
    const progressSpy = jasmine.createSpy();
    const zip = await FileUtils.createZipArchive(
      [
        new File([], 'test_file.txt'),
        new File([], 'test_file_2.txt'),
        new File([], 'test_file_3.txt'),
        new File([], 'test_file_4.txt'),
      ],
      progressSpy,
    );
    expect(zip).toBeInstanceOf(Blob);
    expect(progressSpy).toHaveBeenCalledTimes(4);
    expect(progressSpy).toHaveBeenCalledWith(0.25);
    expect(progressSpy).toHaveBeenCalledWith(0.5);
    expect(progressSpy).toHaveBeenCalledWith(0.75);
    expect(progressSpy).toHaveBeenCalledWith(1);
  });

  it('unzips archive', async () => {
    const validZipFile = await UnitTestUtils.getFixtureFile(
      'traces/winscope.zip',
    );
    const unzippedFiles = await FileUtils.unzipFile(validZipFile);
    expect(unzippedFiles.length).toBe(2);
  });

  it('decompresses gzipped file', async () => {
    const gzippedFile = await UnitTestUtils.getFixtureFile(
      'traces/WindowManager.pb.gz',
    );
    const unzippedFile = await FileUtils.decompressGZipFile(gzippedFile);
    expect(unzippedFile.name).toEqual('traces/WindowManager.pb');
    expect(unzippedFile.size).toEqual(377137);
  });

  it('has download filename regex that accepts all expected inputs', () => {
    expect(FileUtils.DOWNLOAD_FILENAME_REGEX.test('Winscope2')).toBeTrue();
    expect(FileUtils.DOWNLOAD_FILENAME_REGEX.test('win_scope')).toBeTrue();
    expect(FileUtils.DOWNLOAD_FILENAME_REGEX.test('win-scope')).toBeTrue();
    expect(FileUtils.DOWNLOAD_FILENAME_REGEX.test('win.scope')).toBeTrue();
    expect(FileUtils.DOWNLOAD_FILENAME_REGEX.test('win.sc.ope')).toBeTrue();
  });

  it('has download filename regex that rejects all expected inputs', () => {
    expect(FileUtils.DOWNLOAD_FILENAME_REGEX.test('w?n$cope')).toBeFalse();
    expect(FileUtils.DOWNLOAD_FILENAME_REGEX.test('winscope.')).toBeFalse();
    expect(FileUtils.DOWNLOAD_FILENAME_REGEX.test('w..scope')).toBeFalse();
    expect(FileUtils.DOWNLOAD_FILENAME_REGEX.test('wins--pe')).toBeFalse();
    expect(FileUtils.DOWNLOAD_FILENAME_REGEX.test('wi##cope')).toBeFalse();
  });
});
