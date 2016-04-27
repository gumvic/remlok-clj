(enable-console-print!)

(ns remlok.playground
  (:require
    [remlok.loc :refer [mount!]]
    [remlok.playground.counter :refer [root]]))

(mount!
  [root]
  (js/document.getElementById "app"))