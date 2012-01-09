(defproject org.clojars.zentrope/zentrope-wq "0.1.0"
  :description "Simple, managed array-list based worker queues."
  :run-aliases {:doubler zentrope-wq.examples.doubler
                :train zentrope-wq.examples.train }
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/tools.logging"0.2.3"]]
  :dev-dependencies [[org.slf4j/slf4j-api "1.6.4"]
                     [ch.qos.logback/logback-classic "1.0.0"]]
  :clean-non-project-classes true
  :jar-exclusions [#"examples" #".DS_Store"]
  :extra-files-to-clean ["pom.xml" "lib"])
