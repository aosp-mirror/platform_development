;;;
;;; Copyright (C) 2009 The Android Open Source Project
;;;
;;; Licensed under the Apache License, Version 2.0 (the "License");
;;; you may not use this file except in compliance with the License.
;;; You may obtain a copy of the License at
;;;
;;;      http://www.apache.org/licenses/LICENSE-2.0
;;;
;;; Unless required by applicable law or agreed to in writing, software
;;; distributed under the License is distributed on an "AS IS" BASIS,
;;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;;; See the License for the specific language governing permissions and
;;; limitations under the License.

;;; Variables to customize and common function for the android build
;;; support in Emacs.

(defgroup android nil
  "Support for android development in Emacs."
  :group 'tools)

;;;###autoload
(defcustom android-compilation-jobs 2
  "Number of jobs used to do a compilation (-j option of make)."
  :type 'integer
  :group 'android)

(defun android-find-build-tree-root ()
  "Ascend the current path until the root of the android build tree is found.
Similarly to the shell functions in envsetup.sh, for the root both ./Makefile
and ./build/core/envsetup.mk are exiting files.
Return the root of the build tree. Signal an error if not found."
  (let ((default-directory default-directory))
    (while (and (> (length default-directory) 2)
                (not (file-exists-p (concat default-directory "Makefile")))
                (not (file-exists-p (concat default-directory "build/core/envsetup.mk"))))
      (setq default-directory
            (substring default-directory 0
                       (string-match "[^/]+/$" default-directory))))
    (if (> (length default-directory) 2)
        default-directory
      (error "Not in a valid android tree."))))

(defun android-project-p ()
"Return nil if not in an android build tree."
  (condition-case nil
      (android-find-build-tree-root)
    (error nil)))

(provide 'android-common)
