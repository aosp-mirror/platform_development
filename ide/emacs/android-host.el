;;; android-host.el --- Module to use host binaries from an Android dev tree.
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
;; This module defines interactive functions to send the most common
;; commands to a device.
;;
;; Currently only one device is supported.
;;
;; In your .emacs load this file (e.g (require 'android-host)) then
;; you can either create new shortcuts e.g:
;;
;;   (global-set-key [f8] 'android-adb-sync)
;;
;; or rely on autocompletion M-x and-sync will expand to
;; M-x  android-adb-sync
;;
;; By default the following key bindings are active:
;; C-x a a android-adb-root
;; C-x a r android-adb-remount
;; C-x a s android-adb-sync
;; C-x a b android-adb-shell-reboot-bootloader
;; C-x a f android-fastboot-flashall
;;
;; android-fastboot-flashall is still work in progress, check the
;; associated buffer (*Android Output*) for errors when you use it.

;;; Code:

(require 'android-common)

(defvar android-host-command-map (make-sparse-keymap))

(defun android-host-key-prefix-set (var val)
  "Bind the keys shortcuts to the functions.i"
  ;; TODO: This should go in a minor mode keymap instead of
  ;; messing with the global one.
  (define-key global-map (read-kbd-macro val) android-host-command-map)
  (custom-set-default var val))

(let ((map android-host-command-map))
  (define-key map (kbd "a") 'android-adb-root)
  (define-key map (kbd "r") 'android-adb-remount)
  (define-key map (kbd "s") 'android-adb-sync)
  (define-key map (kbd "b") 'android-adb-shell-reboot-bootloader)
  (define-key map (kbd "f") 'android-fastboot-flashall))

(defcustom android-host-key-prefix "C-x a"
  "Prefix keystrokes for Android commands."
  :group 'android
  :type 'string
  :set 'android-host-key-prefix-set)

(defun android-adb-remount ()
  "Execute 'adb remount'."
  (interactive)
  (android-adb-command "remount"))

(defun android-adb-root ()
  "Execute 'adb root'."
  (interactive)
  (android-adb-command "root"))

(defun android-adb-shell-reboot-bootloader ()
  "Execute 'adb shell reboot bootloader'."
  (interactive)
  (android-adb-shell-command "reboot bootloader"))

(defun android-adb-sync ()
  "Execute 'adb sync'."
  (interactive)
  ;; Always force root and remount, this way sync always works even on
  ;; a device that has just rebooted or that runs a userdebug build.
  (android-adb-root)
  (android-adb-remount)
  (android-adb-command "sync" 'p))

(defun android-fastboot-sentinel (process event)
  "Called when the fastboot process is done."
  ;; TODO: Should barf if the last lines are not:
  ;;   OKAY
  ;;   rebooting...
  (princ
   (format "Process: %s had the event `%s'" process event)))

(defun android-fastboot-flashall (arg)
  "Execute 'fastboot -p <product> flashall'.

With no ARG, don't wipe the user data.
With ARG, wipe the user data."
  (interactive "P")
  (when (get-buffer android-output-buffer-name)
    (with-current-buffer android-output-buffer-name
      (erase-buffer)))
  (let ((proc
         (if arg
             (start-process-shell-command
              "fastboot"
              android-output-buffer-name
              (concat (android-fastboot) " flashall -w"))
           (start-process-shell-command
            "fastboot" android-output-buffer-name
            (concat (android-fastboot) " flashall")))))
    (set-process-sentinel proc 'android-fastboot-sentinel)))


(provide 'android-host)
;;; android-host.el ends here
