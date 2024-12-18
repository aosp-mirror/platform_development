/*
 * Copyright (C) 2023 The Android Open Source Project
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
import JSZip from 'jszip';
import {ArrayUtils} from './array_utils';
import {FunctionUtils, OnProgressUpdateType} from './function_utils';

export type OnFile = (file: File, parentArchive: File | undefined) => void;

export class FileUtils {
  //allow: letters/numbers/underscores with delimiters . - # (except at start and end)
  static readonly DOWNLOAD_FILENAME_REGEX = /^\w+?((|#|-|\.)\w+)+$/;
  static readonly ILLEGAL_FILENAME_CHARACTERS_REGEX = /[^A-Za-z0-9-#._]/g;

  static getFileExtension(filename: string): string | undefined {
    const lastDot = filename.lastIndexOf('.');
    if (lastDot === -1) {
      return undefined;
    }
    return filename.slice(lastDot + 1);
  }

  static removeDirFromFileName(name: string): string {
    if (name.includes('/')) {
      const startIndex = name.lastIndexOf('/') + 1;
      return name.slice(startIndex);
    } else {
      return name;
    }
  }

  static removeExtensionFromFilename(name: string): string {
    if (name.includes('.')) {
      const lastIndex = name.lastIndexOf('.');
      return name.slice(0, lastIndex);
    } else {
      return name;
    }
  }

  static async createZipArchive(
    files: File[],
    progressCallback?: OnProgressUpdateType,
  ): Promise<Blob> {
    const zip = new JSZip();
    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      const blob = await file.arrayBuffer();
      zip.file(file.name, blob);
      if (progressCallback) progressCallback((i + 1) / files.length);
    }
    return await zip.generateAsync({type: 'blob'});
  }

  static async unzipFile(
    file: Blob,
    onProgressUpdate: OnProgressUpdateType = FunctionUtils.DO_NOTHING,
  ): Promise<File[]> {
    const unzippedFiles: File[] = [];
    const zip = new JSZip();
    const content = await zip.loadAsync(file);

    const filenames = Object.keys(content.files);
    for (const [index, filename] of filenames.entries()) {
      const file = content.files[filename];
      if (file.dir) {
        // Ignore directories
        continue;
      } else {
        const fileBlob = await file.async('blob');
        const unzippedFile = new File([fileBlob], filename);
        if (await FileUtils.isZipFile(unzippedFile)) {
          unzippedFiles.push(...(await FileUtils.unzipFile(fileBlob)));
        } else {
          unzippedFiles.push(unzippedFile);
        }
      }

      onProgressUpdate((100 * (index + 1)) / filenames.length);
    }

    return unzippedFiles;
  }

  static async decompressGZipFile(file: File): Promise<File> {
    const decompressionStream = new (window as any).DecompressionStream('gzip');
    const decompressedStream = file.stream().pipeThrough(decompressionStream);
    const fileBlob = await new Response(decompressedStream).blob();
    return new File(
      [fileBlob],
      FileUtils.removeExtensionFromFilename(file.name),
    );
  }

  static async isZipFile(file: File): Promise<boolean> {
    return FileUtils.isMatchForMagicNumber(file, FileUtils.PK_ZIP_MAGIC_NUMBER);
  }

  static async isGZipFile(file: File): Promise<boolean> {
    return FileUtils.isMatchForMagicNumber(file, FileUtils.GZIP_MAGIC_NUMBER);
  }

  private static async isMatchForMagicNumber(
    file: File,
    magicNumber: number[],
  ): Promise<boolean> {
    const bufferStart = new Uint8Array((await file.arrayBuffer()).slice(0, 2));
    return ArrayUtils.equal(bufferStart, magicNumber);
  }

  private static readonly GZIP_MAGIC_NUMBER = [0x1f, 0x8b];
  private static readonly PK_ZIP_MAGIC_NUMBER = [0x50, 0x4b];
}
