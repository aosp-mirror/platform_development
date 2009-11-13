;;; android-compile.el --- Compile the Android source tree.
;;
;; Copyright (C) 2009 The Android Open Source Project
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;      http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

;;; Commentary:
;;
;; Helper functions to compile Android file within emacs.
;; This module ignores 'build/envsetup.sh' and any enviroment set by the
;; 'lunch' shell function.
;; Instead it relies solely on 'buildspec.mk', remember that when you
;; switch configuration.
;;
;; The only interactive function is 'android-compile'.
;; In your .emacs load this file (e.g (require 'android-compile)) then:
;;
;;   (add-hook 'c++-mode-hook 'android-compile)
;;   (add-hook 'java-mode-hook 'android-compile)
;; and/or
;;   (global-set-key [f9] 'android-compile)
;;
;;
;; TODO: Maybe we could cache the result of the compile function in
;; buffer local vars.

;;; Code:

(require 'compile)
(require 'android-common)

;; No need to be customized.
(defvar android-compile-ignore-re
  "\\(^\\(\\sw\\|[/_]\\)+\\(Makefile\\|\\.mk\\):[0-9]+:.*warning\\)\\|\\(^/bin/bash\\)"
  "RE to match line to suppress during a compilation.
During the compilation process line matching the above will be
suppressed if `android-compilation-no-buildenv-warning' is non nil.")

(defun android-makefile-exists-p (directory)
  "Return t if an Android makefile exists in DIRECTORY."
  ; Test for Android.mk first: more likely.
  (or (file-exists-p (concat directory "Android.mk"))
      (file-exists-p (concat directory "Makefile"))))

(defun android-find-makefile (topdir)
  "Ascend the current path until an Android makefile is found.
Makefiles are named Android.mk except in the root directory where
the file is named Makefile.
TOPDIR is the root directory of the build.
Return a list with 2 elements (MAKEFILE_PATH IS_ROOT_MAKEFILE).
MAKEFILE_PATH is the relative path of the makefile wrt TOPDIR.
Signal an error if no Makefile was found."
  ;; TODO: Could check that topdir is the start of default-directory.
  (unless (> (length topdir) 2)
    (error "Topdir invalid %s for current dir %s" topdir default-directory))
  (let ((default-directory default-directory)
        file)
    ;; Ascend the path.
    (while (and (> (length default-directory) (length topdir))
                (not (android-makefile-exists-p default-directory)))
      (setq default-directory
            (substring default-directory 0
                       (string-match "[^/]+/$" default-directory))))

    (when (not (android-makefile-exists-p default-directory))
      (error "Not in a valid android tree"))

    (if (string= default-directory topdir)
        (list "Makefile" t)
      ;; Remove the root dir at the start of the filename
      (setq default-directory (substring default-directory (length topdir) nil))
      (setq file (concat default-directory "Android.mk"))
      (list file nil))))

;; This filter is registered as a `compilation-filter-hook' and is
;; called when new data has been inserted in the compile buffer. Don't
;; assume that only one line has been inserted, typically more than
;; one has changed since the last call due to stdout buffering.
;;
;; We store in a buffer local variable `android-compile-context' a
;; list with 2 elements, the process and point position at the end of
;; the last invocation. The process is used to detect a new
;; compilation. The point position is used to limit our search.
;;
;; On entry (point) is at the end of the last block inserted.
(defun android-compile-filter ()
  "Filter to discard unwanted lines from the compilation buffer.

This filter is registered as a `compilation-filter-hook' and is
called when new data has been inserted in the compile buffer.

Has effect only if `android-compilation-no-buildenv-warning' is
not nil."
  ;; Currently we are looking only for compilation warnings from the
  ;; build env. Move this test lower, near the while loop if we
  ;; support more than one category of regexp.
  (when android-compilation-no-buildenv-warning

    ;; Check if android-compile-context does not exist or if the
    ;; process has changed: new compilation.
    (let ((proc (get-buffer-process (current-buffer))))
      (unless (and (local-variable-p 'android-compile-context)
                   (eq proc (cadr android-compile-context)))
        (setq android-compile-context (list (point-min) proc))
        (make-local-variable 'android-compile-context)))

    (let ((beg (car android-compile-context))
          (end (point)))
      (save-excursion
        (goto-char beg)
        ;; Need to go back at the beginning of the line before we
        ;; start the search: because of the buffering, the previous
        ;; block inserted may have ended in the middle of the
        ;; expression we are trying to match. As result we missed it
        ;; last time and we would miss it again if we started just
        ;; where we left of. By processing the line from the start we
        ;; are catching that case.
        (forward-line 0)
        (while (search-forward-regexp android-compile-ignore-re end t)
          ;; Nuke the line
          (let ((bol (point-at-bol)))
            (forward-line 1)
            (delete-region bol (point)))))
      ;; Remember the new end for next time around.
      (setcar android-compile-context (point)))))

(defun android-compile ()
  "Elisp equivalent of mm shell function.
Walk up the path until a makefile is found and build it.
You need to have a proper buildspec.mk in your top dir.

Use `android-compilation-jobs' to control the number of jobs used
in a compilation."
  (interactive)
  (if (android-project-p)
      (let* ((topdir (android-find-build-tree-root))
             (makefile (android-find-makefile topdir))
             (options
              (concat " -j " (number-to-string android-compilation-jobs))))
        (unless (file-exists-p (concat topdir "buildspec.mk"))
          (error "buildspec.mk missing in %s." topdir))
        ;; Add-hook do not re-add if already present. The compile
        ;; filter hooks run after the comint cleanup (^M).
        (add-hook 'compilation-filter-hook 'android-compile-filter)
        (set (make-local-variable 'compile-command)
             (if (cadr makefile)
                 ;; The root Makefile is not invoked using ONE_SHOT_MAKEFILE.
                 (concat "make -C " topdir options) ; Build the whole image.
               (concat "ONE_SHOT_MAKEFILE=" (car makefile)
                       " make -C " topdir options " files ")))
        (if (interactive-p)
            (call-interactively 'compile)))))

(provide 'android-compile)

;;; android-compile.el ends here
