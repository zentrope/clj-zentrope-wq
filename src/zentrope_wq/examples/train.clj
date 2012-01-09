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

(defmacro with-job-control
  [& exprs]
  `(try
     (let [result# (do ~@exprs)]
       result#)
     (catch Throwable t#
       (job-failure!)
       (throw t#))))

(defn- wait-for-completion
  []
  (Thread/sleep 1000)
  (log/info "num-jobs" @num-jobs)
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
  (when (= (rand-int 100) 2)
    (swap! failures conj job)
    (throw (Exception. "simulated failed"))))

;; Worker functions

(defn- query-worker-fn
  [value]
  (with-job-control
    (maybe-fail value)
    (wq/put :process-q (assoc value :queried? true))))

(defn- process-worker-fn
  [value]
  (with-job-control
    (maybe-fail value)
    (wq/put :delete-q (assoc value :processed? true))))

(defn- delete-worker-fn
  [value]
  (with-job-control
    (maybe-fail value)
    (job-success!)
      (swap! successes conj (assoc value :pruned? true))))

;; App lifecycle

(defn- start
  []
  (wq/start :query-q 10 query-worker-fn)
  (wq/start :process-q 10 process-worker-fn)
  (wq/start :delete-q 10 delete-worker-fn))

(defn- stop
  []
  (doseq [q [:query-q :process-q :delete-q]]
    (wq/stop q)))

(defn -main
  [& args]
  (log/info "Hello from Train Example.")
  (set-job-count! 100)
  (start)
  (doseq [x (range 100)]
    (wq/put :query-q {:id x :queried? false :processed? false :pruned? false}))
  (wait-for-completion)
  (log/info "SUCCESSES")
  (doseq [job @successes]
    (log/info "  job.success" job))
  (log/info "FAILURES")
  (doseq [job @failures]
    (log/info "  job.failures" job))
  (stop))
