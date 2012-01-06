(ns zentrope-wq.examples.doubler
  ;;
  ;; Just to demo and develop with. Not included in published
  ;; artifact.
  ;;
  (:require [zentrope-wq.core :as wq]
            [clojure.tools.logging :as log]))

(def ^:private num-workers 5)
(def ^:private results (atom []))

(defn- doubler-fn
  [value]
  (log/info "doubling" value)
  (swap! results conj (* value 2)))

(defn- start
  []
  (wq/start-queue :doubler-q num-workers doubler-fn))

(defn- stop
  []
  (wq/stop-queue :doubler-q))

(defn -main
  [& args]
  (log/info "Hello from Doubler Example.")
  (start)
  (let [src (range 100)]
    (log/info "inputs" src)
    (doseq [s src]
      (wq/put :doubler-q s))
    (log/info "pausing....")
    (Thread/sleep 4000)
    (log/info "outputs" @results)
    (stop)))
