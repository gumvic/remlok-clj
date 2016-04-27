(enable-console-print!)

(ns remlok.playground
  (:require
    [remlok.loc :refer [mount!]]
    [remlok.playground.hello :refer [root main]]))

(mount! main root (js/document.getElementById "app"))