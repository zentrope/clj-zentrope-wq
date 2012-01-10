(ns zentrope-wq.examples.exceptions
  ;;
  ;; Shows the use of error handlers for when side-effect workers blow
  ;; up. Runs a series of integers through the queues occasionally
  ;; throwing exceptions, then presents a report of the successes and
  ;; failures.
  ;;
  (:require [zentrope-wq.worker-queues :as wq]
            [clojure.tools.logging :as log]))

(def ^:private successes (atom 0))
(def ^:private errors (atom 0))
(def ^:private error-jobs (agent []))

(defn- on-error
  [qname job exception]
  (swap! errors inc)
  (send error-jobs conj job))

(defn- on-success
  [job]
  ;; Throws an exception on every integer disible by 5.
  (if (= 0 (mod job 5))
    (throw (Exception. "error.5")))
  (swap! successes inc))

(defn -main
  [& args]
  (log/info "Hello from Exceptions Example")
  ;;
  ;; Start the queue
  ;;
  (wq/start :testq 10 on-success :qsize 100 :error-handler on-error)
  ;;
  ;; Pump numbers into the queue
  ;;
  (log/info "Pumping jobs.")
  (doseq [x (range 500)]
    (wq/put :testq x))
  ;;
  ;; Wait a bit.
  ;;
  (log/info "Waiting for jobs to complete.")
  (Thread/sleep 3000)
  ;;
  ;; Print out the report.
  ;;
  (log/info "Report:" {:successes @successes :errors-count @errors})
  (log/info " Failed jobs:" (count @error-jobs) "values -> " @error-jobs)
  ;;
  ;; Stop the queue
  ;;
  (wq/stop :testq)
  ;;
  (log/info "Done!")
  (shutdown-agents)
  (System/exit 0))
