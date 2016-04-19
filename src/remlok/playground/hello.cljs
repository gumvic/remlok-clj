(ns remlok.playground.hello
  (:require
    [remlok.loc :refer [app ui]]))

(def root
  (ui
    {:render
     (fn []
       (println "Have I just rendered twice?")
       [:span "Hello !!!"])}))

(def main
  (app {}))