(defproject web-stats "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :dev-dependencies [[swank-clojure "1.4.0-SNAPSHOT"]]
  :local-repo-classpath true
  :dev-resources-path "dev-resources"
  :main web-stats.main
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [com.relaynetwork/perceptor "1.0.0-SNAPSHOT"]
                 [com.github.kyleburton/teporingo "2.1.16"]
                 [noir "1.1.0"]])
