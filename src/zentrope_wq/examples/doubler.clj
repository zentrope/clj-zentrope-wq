(ns zentrope-wq.examples.doubler
  ;;
  ;; Trivial example of workers that double the value
  ;; found in a queue and add it to a mutable var.
  ;;
  (:require [zentrope-wq.worker-queues :as wq]
            [clojure.tools.logging :as log]))

(def ^:private num-workers 5)
(def ^:private results (atom []))

(defn- doubler-worker-fn
  [value]
  (log/info "doubling" value)
  (swap! results conj (* value 2)))

(defn- start
  []
  (wq/start :doubler-q num-workers doubler-worker-fn))

(defn- stop
  []
  (wq/stop :doubler-q))

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
