(defproject org.clojars.zentrope/zentrope-wq "0.2.0"

  :description "Simple, managed array-list-based worker queues."

  :run-aliases {:doubler zentrope-wq.examples.doubler
                :train zentrope-wq.examples.train
                :exceptions zentrope-wq.examples.exceptions }

  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/tools.logging "0.2.3"]]

  :dev-dependencies [[org.slf4j/slf4j-api "1.6.4"]
                     [ch.qos.logback/logback-classic "1.0.0"]
                     [clj-stacktrace "0.2.4"]]

  :clean-non-project-classes true

  :jar-exclusions [#"examples" #".DS_Store" #"resources"]

  :extra-files-to-clean ["pom.xml" "lib"])
