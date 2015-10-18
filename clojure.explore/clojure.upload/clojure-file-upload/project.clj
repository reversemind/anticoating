(defproject clojure-file-upload "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
                 [org.clojure/clojure "1.7.0"]
                 [compojure "1.4.0"]
                 [http-kit "2.1.19"]
                 [org.clojure/tools.logging "0.3.1"]
                 [ch.qos.logback/logback-classic "1.1.3"]
                 ]
  :main ^:skip-aot clojure-file-upload.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
