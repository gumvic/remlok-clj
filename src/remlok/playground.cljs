(ns remlok.playground
  (:require
    [remlok.loc :refer [mount!]]
    [remlok.playground.hello :refer [root main]]))

(enable-console-print!)

(mount! main root (js/document.getElementById "app"))