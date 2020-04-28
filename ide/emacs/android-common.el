;;; android-common.el --- Common functions/variables to dev Android in Emacs.
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
;; Variables to customize and common functions for the Android build
;; support in Emacs.
;; There should be no interactive function in this module.
;;
;; You need to have a proper buildspec.mk file in your root directory
;; for this module to work (see $TOP/build/buildspec.mk.default).
;; If the path the product's files/image uses an a product alias, you
;; need to add a mapping in `android-product-alias-map'. For instance
;; if TARGET_PRODUCT is foo but the build directory is out/target/product/bar,
;; you need to add a mapping Target:foo -> Alias:bar
;;

;;; Code:

(defgroup android nil
  "Support for android development in Emacs."
  :prefix "android-"                    ; Currently unused.
  :tag    "Android"
  :group  'tools)

;;;###autoload
(defcustom android-compilation-jobs 2
  "Number of jobs used to do a compilation (-j option of make)."
  :type 'integer
  :group 'android)

;;;###autoload
(defcustom android-compilation-no-buildenv-warning t
  "If not nil, suppress warnings from the build env (Makefile,
bash) from the compilation output since they interfere with
`next-error'."
  :type 'boolean
  :group 'android)

;;;###autoload
(defcustom android-product-alias-map nil
  "Alist between product targets (declared in buildspec.mk) and actual
 product build directory used by `android-product'.

For instance if TARGET_PRODUCT is 'foo' but the build directory
 is 'out/target/product/bar', you need to add a mapping Target:foo -> Alias:bar."
  :type '(repeat (list (string :tag "Target")
                       (string :tag "Alias")))
  :group 'android)

(defconst android-output-buffer-name "*Android Output*"
  "Name of the default buffer for the output of the commands.
There is only one instance of such a buffer.")

(defun android-find-build-tree-root ()
  "Ascend the current path until the root of the android build tree is found.
Similarly to the shell functions in envsetup.sh, for the root both ./Makefile
and ./build/core/envsetup.mk are exiting files.
Return the root of the build tree.  Signal an error if not found."
  (let ((default-directory default-directory))
    (while (and (> (length default-directory) 2)
                (not (file-exists-p (concat default-directory
                                            "Makefile")))
                (not (file-exists-p (concat default-directory
                                            "build/core/envsetup.mk"))))
      (setq default-directory
            (substring default-directory 0
                       (string-match "[^/]+/$" default-directory))))
    (if (> (length default-directory) 2)
        default-directory
      (error "Not in a valid android tree"))))

(defun android-project-p ()
  "Return nil if not in an android build tree."
  (condition-case nil
      (android-find-build-tree-root)
    (error nil)))

(defun android-host ()
  "Return the <system>-<arch> string (e.g linux-x86).
Only linux and darwin on x86 architectures are supported."
  (or (string-match "x86" system-configuration)
      (string-match "i386" system-configuration)
      (error "Unknown arch"))
  (or (and (string-match "darwin" system-configuration) "darwin-x86")
      (and (string-match "linux" system-configuration) "linux-x86")
      (error "Unknown system")))

(defun android-product ()
  "Return the product built according to the buildspec.mk.
You must have buildspec.mk file in the top directory.

Additional product aliases can be listed in `android-product-alias-map'
if the product actually built is different from the one listed
in buildspec.mk"
  (save-excursion
    (let* ((buildspec (concat (android-find-build-tree-root) "buildspec.mk"))
           (product (with-current-buffer (find-file-noselect buildspec)
                      (goto-char (point-min))
                      (search-forward "TARGET_PRODUCT:=")
                      (buffer-substring-no-properties (point)
                                                      (scan-sexps (point) 1))))
           (alias (assoc product android-product-alias-map)))
      ; Post processing, adjust the names.
      (if (not alias)
          product
        (nth 1 alias)))))

(defun android-product-path ()
  "Return the full path to the product directory.

Additional product aliases can be added in `android-product-alias-map'
if the product actually built is different from the one listed
in buildspec.mk"
  (let ((path (concat (android-find-build-tree-root) "out/target/product/"
                      (android-product))))
    (when (not (file-exists-p path))
      (error (format "%s does not exist. If product %s maps to another one,
add an entry to android-product-map." path (android-product))))
    path))

(defun android-find-host-bin (binary)
  "Return the full path to the host BINARY.
Binaries don't depend on the device, just on the host type.
Try first to locate BINARY in the out/host tree.  Fallback using
the shell exec PATH setup."
  (if (android-project-p)
      (let ((path (concat (android-find-build-tree-root) "out/host/"
                          (android-host) "/bin/" binary)))
        (if (file-exists-p path)
            path
          (error (concat binary " is missing."))))
    (executable-find binary)))

(defun android-adb ()
  "Return the path to the adb executable.
If not in the build tree use the PATH env variable."
  (android-find-host-bin "adb"))

(defun android-fastboot ()
  "Return the path to the fastboot executable.
If not in the build tree use the PATH env variable."
  ; For fastboot -p is the name of the product, *not* the full path to
  ; its directory like adb requests sometimes.
  (concat (android-find-host-bin "fastboot") " -p " (android-product)))

(defun android-adb-command (command &optional product)
  "Execute 'adb COMMAND'.
If the optional PRODUCT is not nil, -p (android-product-path) is used
when adb is invoked."
  (when (get-buffer android-output-buffer-name)
    (with-current-buffer android-output-buffer-name
      (erase-buffer)))
  (if product
      (shell-command (concat (android-adb) " -p " (android-product-path)
                             " " command)
                     android-output-buffer-name)
    (shell-command (concat (android-adb) " " command)
                   android-output-buffer-name)))

(defun android-adb-shell-command (command)
  "Execute 'adb shell COMMAND'."
  (android-adb-command (concat " shell " command)
                       android-output-buffer-name))

(provide 'android-common)

;;; android-common.el ends here
