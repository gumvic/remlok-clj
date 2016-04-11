(defproject remlok "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [org.clojure/core.async "0.2.374"]
                 [com.cognitect/transit-clj "0.8.285"]
                 [funcool/promesa "0.8.1"]
                 [rum "0.6.0"]]
  :plugins [[lein-cljsbuild "1.1.2"]]
  :cljsbuild
  {:builds
   {:dev
    {:source-paths ["src"]
     :compiler {:main remlok.playground
                :output-dir "target"
                :output-to "target/main.js"
                :optimizations :none
                :source-map true
                :pretty-print true}}}})
