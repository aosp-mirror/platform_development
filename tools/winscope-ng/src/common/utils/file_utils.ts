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
import JSZip from "jszip";

class FileUtils {
  static async createZipArchive(files: File[]): Promise<Blob> {
    const zip = new JSZip();
    for (let i=0; i < files.length; i++) {
      const file = files[i];
      const blob = await file.arrayBuffer();
      zip.file(file.name, blob);
    }
    return await zip.generateAsync({type: "blob"});
  }

  static async readFile(file: File): Promise<Uint8Array> {
    return await new Promise((resolve, _) => {
      const reader = new FileReader();
      reader.onload = async (e) => {
        const buffer = new Uint8Array(e.target!.result as ArrayBuffer);
        resolve(buffer);
      };
      reader.readAsArrayBuffer(file);
    });
  }

  static isZipFile(file: File) {
    return this.getFileExtension(file) === "zip";
  }

  static getFileExtension(file: File) {
    const split = file.name.split(".");
    if (split.length > 1) {
      return split.pop();
    }
    return undefined;
  }

  static removeDirFromFileName(name: string) {
    if (name.includes("/")) {
      const startIndex = name.lastIndexOf("/") + 1;
      return name.slice(startIndex);
    } else {
      return name;
    }
  }

  static async unzipFile(file: File): Promise<File[]> {
    const unzippedFiles: File[] = [];
    const buffer: Uint8Array = await this.readFile(file);
    const zip = new JSZip();
    const content = await zip.loadAsync(buffer);
    for (const filename in content.files) {
      const file = content.files[filename];
      if (file.dir) {
        // Ignore directories
        continue;
      } else {
        const name = this.removeDirFromFileName(filename);
        const fileBlob = await file.async("blob");
        const unzippedFile = new File([fileBlob], name);
        unzippedFiles.push(unzippedFile);
      }
    }
    return unzippedFiles;
  }
}

export {FileUtils};
