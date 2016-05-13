(enable-console-print!)

(ns remlok.playground
  (:require
    [reagent.core :refer [render]]
    [remlok.loc :refer [sub]]
    [remlok.playground.wiki :refer [root]]))

(render
  [root]
  (js/document.getElementById "app"))