(ns zentrope-wq.examples.train
  ;;
  ;;
  (:require [zentrope-wq.core :as wq]
            [clojure.tools.logging :as log]))

;; Some lightweight batch job control so that
;; we don't send any more jobs through the train
;; of Queues until we're done.

(def ^:private num-jobs (atom 0))
(def ^:private successes (atom 0))
(def ^:private failures (atom 0))

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

(def ^:private successes (atom []))
(def ^:private failures (atom []))

(defn- maybe-fail
  "Simulates a side-effect kind of failure in processing
a job in a queue. If there's a failure, add the job to the
collection of failed jobs."
  [job]
  (when (= (rand-int 100) 2)
    (swap! failures conj job)
    (throw (Exception. "simulated failed"))))

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

(defn- start
  []
  (wq/start :query-q 10 query-worker-fn)
  (wq/start :process-q 10 process-worker-fn)
  (wq/start :delete-q 10 delete-worker-fn))

(defn- stop
  []
  (doseq [q [:query-q :process-q :delete-q]]
    (wq/stop q)))

(defn- wait-for-completion
  []
  (Thread/sleep 1000)
  (log/info "num-jobs" @num-jobs)
  (when-not (jobs-complete?)
    (recur)))

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
