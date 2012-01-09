(ns zentrope-wq.worker-queues
  (:import [java.util.concurrent ArrayBlockingQueue TimeUnit])
  (:require [clojure.tools.logging :as log]))

;; DEV-NOTE: All this thread management stuff needs to be re-implemented
;; using all that util.Executors stuff.

(def ^:private queue-size 1000)
(def ^:private queues (atom {}))

(defn- break?
  [q]
  (when (= (deref (:lock q) 100 :keep-going) :exit)
    (throw (InterruptedException. "break"))))

(defn- poll
  [q]
  (let [value (.poll (:queue q) (:wait q) (TimeUnit/SECONDS))]
    (break? q)
    value))

(defn- delegate
  [q value]
  (when-let [func (:delegate q)]
    (func value)))

(defn- poll-loop
  [q]
  (try
    (loop []
      (try
        (when-let [value (poll q)]
          (delegate q value))
        (catch Throwable t
          (if (instance? InterruptedException t)
            (throw t)
            (log/error (:qname q) t))))
      (recur))
    (catch Throwable t
      (log/warn (:qname q) t))))

(defn- mk-worker
  [q index]
  (let [thread-name (str (name  (:qname q)) ".worker." index)]
    (log/info "creating worker" thread-name)
    (doto (Thread. (fn [] (poll-loop q)))
      (.setName thread-name)
      (.start))))

(defn- mk-workers
  [q]
  (let [workers (for [x (range (:num-workers q))] (mk-worker q x))]
    (assoc q :workers (doall workers))))

(defn- mk-queue
  [qname qsize handler num-workers]
  (mk-workers {:queue (ArrayBlockingQueue. qsize)
               :qname qname
               :wait 1
               :lock (promise)
               :size qsize
               :num-workers num-workers
               :delegate handler
               :workers nil}))

;; ----------------------------------------------------------------------------
;; Public
;; ----------------------------------------------------------------------------

(defn put
  "Puts the VALUE to the named queue and blocks if the queue is full."
  [queue-name value]
  (when-let [thread (queue-name @queues)]
    (when-let [q (:queue thread)]
      (.put q value))))

(defn stop
  "Empty the queue and destroy all the workers."
  [queue-name]
  (if-let [q (queue-name @queues)]
    (do
      (deliver (:lock q) :exit)
      (.clear (:queue q))
      (swap! queues dissoc queue-name)
      :stopped)
    :queue-not-found))

(defn start
  "Create a queue, spawn num-workers worker-fns to eat from the queue."
  [queue-name num-workers worker-fn]
  (let [q (mk-queue queue-name queue-size worker-fn num-workers)]
    (swap! queues assoc queue-name q)
    queue-name))
