(ns zentrope-wq.examples.train
  ;;
  ;; A complicated example in which a hash-map is moved
  ;; from queue to queue, with "successes" and "failures"
  ;; monitored by a "control" such that we can tell how
  ;; a given batch of jobs has fared.
  ;;
  (:require [zentrope-wq.worker-queues :as wq]
            [clojure.tools.logging :as log]))

;; Job control stuff

(def ^:private num-jobs (atom 0))

(defn- set-job-count!
  [count]
  (reset! num-jobs count))

(defn- job-success!
  []
  (swap! num-jobs dec))

(defn- job-failure!
  []
  (swap! num-jobs dec))

(defn- jobs-complete?
  []
  (= 0 @num-jobs))

(defn- wait-for-completion
  []
  (Thread/sleep 1000)
  (when-not (jobs-complete?)
    (recur)))

(def ^:private successes (atom []))
(def ^:private failures (atom []))

;; Simulate failures due to side effects.

(defn- maybe-fail
  "Simulates a side-effect kind of failure in processing
a job in a queue. If there's a failure, add the job to the
collection of failed jobs."
  [job]
  (when (= (rand-int 20) 2)
    (throw (Exception. "fake-failure"))))

;; Error handling function

(defn- handle-errors
  [qname job exception]
  (job-failure!)
  (swap! failures conj (assoc job :failed-on qname :exception (.getMessage exception))))

;; Worker functions

(defn- query-worker-fn
  [value]
  (maybe-fail value)
  (wq/put :process-q (assoc value :queried? true)))

(defn- process-worker-fn
  [value]
  (maybe-fail value)
  (wq/put :delete-q (assoc value :processed? true)))

(defn- delete-worker-fn
  [value]
  (maybe-fail value)
  (job-success!)
  (swap! successes conj (assoc value :pruned? true)))

;; App lifecycle

(defn- start
  []
  (wq/start :query-q   10 query-worker-fn     :qsize 100 :error-handler handle-errors)
  (wq/start :process-q 10 process-worker-fn   :qsize 100 :error-handler handle-errors)
  (wq/start :delete-q  10 delete-worker-fn    :qsize 100 :error-handler handle-errors))

(defn- stop
  []
  (doseq [q [:query-q :process-q :delete-q]]
    (wq/stop q)))

(def ^:private jobs-to-run 100)

(defn -main
  [& args]
  (log/info "Hello from Train Example.")
  ;;
  (log/info "Pushing 100 jobs through the queue train.")
  (set-job-count! jobs-to-run)
  (start)
  (doseq [x (range jobs-to-run)]
    (wq/put :query-q {:id x :queried? false :processed? false :pruned? false}))
  ;;
  ;;
  (wait-for-completion)
  ;;
  ;; Report
  ;;
  (log/info "SUCCESSES")
  (doseq [job @successes]
    (log/info "  job.success " job))
  (log/info "FAILURES (check out how far job got before failing)")
  (doseq [job @failures]
    (log/info "  job.failures" job))
  (log/info "TOTAL" (+ (count @successes) (count @failures)) "expected" jobs-to-run)
  ;;
  ;; Done
  ;;
  (stop))
