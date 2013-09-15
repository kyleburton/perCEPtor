(defproject com.github.kyleburton/perceptor "1.0.0"
  :description "Clojure bindings for Esper: http://esper.codehaus.org/"
  :url         "http://github.com/kyleburton/perCEPtor"
  :lein-release {:deploy-via :clojars :scm :git}
  :license {:name         "Eclipse Public License - v 1.0"
            :url          "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments     "Same as Clojure"}
  :repositories         {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}
  :java-source-path     "java"
  :local-repo-classpath true

  :profiles             {:dev {:dependencies [[swank-clojure "1.4.3"]]}
                         :1.2 {:dependencies [[org.clojure/clojure "1.2.0"]]}
                         :1.3 {:dependencies [[org.clojure/clojure "1.3.0"]]}
                         :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
                         :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
                         :1.6 {:dependencies [[org.clojure/clojure "1.6.0-master-SNAPSHOT"]]}}
  :aliases              {"all" ["with-profile" "dev,1.2:dev,1.3:dev,1.4:dev,1.5:dev,1.6"]}
  :global-vars          {*warn-on-reflection* true}
  :dependencies         [[com.espertech/esper                         "4.7.0"]
                         [com.github.kyleburton/clj-etl-utils         "1.0.76"]
                         [com.relaynetwork/clorine                    "1.0.14"]
                         ])
