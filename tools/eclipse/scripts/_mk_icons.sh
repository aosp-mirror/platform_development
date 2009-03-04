#!/bin/bash

function icon() {
  # $1=letter, $2=letter's color (e.g. A red), $3=filename
  ./gen_icon.py ${3}.png 16 white black $2 $1
}

icon M green  manifest
  icon S blue   sharedUserId
  icon S red    signature
  icon P green  package

icon I green  instrumentation
  icon F green  functionalTest
  icon H green  handleProfiling
  icon I green  icon
  icon T green  targetPackage

icon U blue   uses-permission
icon P red    permission
  icon N green  name
  icon L blue   label

icon A blue     application
    icon P red    permission
    icon P blue   persistent
    icon P green  process
    icon T green  taskAffinity
    icon T blue   theme
  icon P red    provider
    icon A green  authorities
    icon I green  initOrder
    icon M green  multiprocess
    icon R green  readPermission
    icon W green  writePermission
    icon S green  syncable
  icon R green  receiver
  icon S blue   service
  icon A green  activity
      icon C blue   clearOnBackground
      icon C green  configChanges
      icon E green  excludeFromRecents
      icon L green  launchMode
      icon S green  stateNotNeeded
    icon F blue  intent-filter
        icon P green  priority
      icon A red    action
      icon C green  category
      icon D green  data
        icon M green    mimeType
        icon S green    scheme
        icon H green    host
        icon P green    port
        icon P blue     path

