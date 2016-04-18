(ns remlok.playground.hello
  (:require
    [remlok.loc :refer [app ui]]))

(def root
  (ui
    {:render
     (fn []
       [:span "Hello !!!"])}))

(def main
  (app {}))