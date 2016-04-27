(enable-console-print!)

(ns remlok.playground
  (:require
    [remlok.loc :refer [mount!]]
    [remlok.playground.hello :refer [root]]))

(mount!
  [root]
  (js/document.getElementById "app"))