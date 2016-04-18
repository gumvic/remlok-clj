(enable-console-print!)

(ns remlok.playground
  (:require
    [remlok.loc :refer [mount!]]
    [remlok.playground.counter :refer [root main]]))

(mount! main root (js/document.getElementById "app"))